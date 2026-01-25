package com.cosmic.scavengers.engine;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.ecs.queue.EcsCommandQueueProcessing;
import com.cosmic.scavengers.system.IntentProcessorSystem;
import com.cosmic.scavengers.system.MovementSystem;

/**
 * The core game loop component. Runs on a dedicated thread, executes ECS
 * systems, and broadcasts the resulting game state.
 *
 * Note: MovementSystem currently expects a TICK_DELTA of 0.1s (100ms). The
 * GameEngine's TICK_RATE_MS must match that delta to avoid accelerated
 * movement.
 */
@Component
public class GameEngine implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(GameEngine.class);
	// The fixed step our systems expect (0.1 seconds)
	private static final double TICK_DELTA_S = 0.1;
	// Convert that step to nanoseconds for precise comparison
	private static final long TICK_DELTA_NS = (long) (TICK_DELTA_S * 1_000_000_000L);

	private final List<Runnable> systems = new java.util.ArrayList<>();

	private boolean running = true;

	public GameEngine(
			EcsCommandQueueProcessing commandHandlerSystem,
			IntentProcessorSystem intentProcessorSystem,
			MovementSystem movementSystem) {
		systems.add(commandHandlerSystem);		
		systems.add(intentProcessorSystem);
		systems.add(movementSystem);
	}

	@Override
	public void run() {
		log.info("GameEngine started with Accumulator Timestep ({}s per tick)", TICK_DELTA_S);

		long lastTime = System.nanoTime();
		long accumulator = 0;

		while (running && !Thread.currentThread().isInterrupted()) {
			long currentTime = System.nanoTime();
			long frameTime = currentTime - lastTime;
			lastTime = currentTime;

			// Cap frameTime to avoid "Spiral of Death" if server hangs for seconds
			if (frameTime > 250_000_000L) { // max 0.25s per loop
				frameTime = 250_000_000L;
			}

			accumulator += frameTime;
			// Consume all accumulated time in fixed TICK_DELTA_NS steps
			while (accumulator >= TICK_DELTA_NS) {
				executeGameTick();
				accumulator -= TICK_DELTA_NS;
			}

			// Sleep a tiny bit to prevent 100% CPU usage
			// This is "relaxed" sleep; the accumulator handles the precision
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void executeGameTick() {
		try {
			for (Runnable system : systems) {
				system.run();
			}

			// Note: In this pattern, we broadcast after the systems run
			// If you want to save bandwidth, you can move stateBroadcaster
			// outside the while loop so it only sends the latest state after
			// all catch-up ticks are done.
			// stateBroadcaster.broadcastCurrentState(dominion);
		} catch (Exception e) {
			log.error("Error in game tick execution", e);
			this.stop();
		}
	}

	public void stop() {
		this.running = false;
	}
}
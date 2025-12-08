package com.cosmic.scavengers.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.broadcast.IStateBroadcaster;
import com.cosmic.scavengers.system.MovementSystem;

import dev.dominion.ecs.api.Dominion;

/**
 * The core game loop component. Runs on a dedicated thread, executes ECS
 * systems, and broadcasts the resulting game state. It is correctly annotated
 * as a @Component because it is a central service/worker.
 */
@Component
public class GameEngine implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(GameEngine.class);
	
	private static final int TICK_RATE_MS = 1000 / 20;

	private final Dominion dominion;
	private final MovementSystem movementSystem;
	private final IStateBroadcaster stateBroadcaster;

	// Spring injects Dominion (from GameConfig), MovementSystem (Component),
	// and IStateBroadcaster (from NetworkingConfig).
	public GameEngine(Dominion dominion, MovementSystem movementSystem, IStateBroadcaster stateBroadcaster) {
		this.dominion = dominion;
		this.movementSystem = movementSystem;
		this.stateBroadcaster = stateBroadcaster;
	}

	@Override
	public void run() {
		log.info("GameEngine thread started. Entering fixed-timestep game loop...");

		long nextGameTick = System.currentTimeMillis();
		
		while (!Thread.currentThread().isInterrupted()) {
			long currentTime = System.currentTimeMillis();

			// 1. Wait until the next scheduled tick time
			long sleepTime = nextGameTick - currentTime;
			if (sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt(); // Restore interrupt status
					log.info("GameEngine thread interrupted and shutting down.");
					break;
				}
			} else {
				// If the loop is running behind, log a warning
				if (currentTime > nextGameTick + TICK_RATE_MS) {
					log.warn("Game tick took too long and fell behind schedule by {}ms.", currentTime - nextGameTick);
				}
			}
			executeGameTick();
			nextGameTick += TICK_RATE_MS;
		}
	}

	private void executeGameTick() {
		try {
			movementSystem.run();
		} catch (Exception e) {
			log.error("An error occurred during ECS system execution.", e);
		}
		
		try {
			stateBroadcaster.broadcastCurrentState(dominion);
		} catch (Exception e) {
			log.error("An error occurred during state broadcast.", e);
		}
	}
}
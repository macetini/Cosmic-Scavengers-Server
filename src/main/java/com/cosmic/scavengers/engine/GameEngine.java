package com.cosmic.scavengers.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.system.MovementSystem;

import dev.dominion.ecs.api.Dominion;

/**
 * Placeholder for the main game loop logic. Implements Runnable so it can be
 * started in a separate thread by NettyServerInitializer.
 */
@Component
public class GameEngine implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(GameEngine.class);

	// Renamed field to match the new interface
	private final Dominion dominion;

	public GameEngine() {
		// 1. Initialize the Dominion ECS Context
		this.dominion = Dominion.create();

		MovementSystem moveSystem;
	}

	@Override
	public void run() {
		log.info("GameEngine thread started. Entering game loop...");
		// Implement your game loop logic here (e.g., tick rate, state updates)
		try {
			while (!Thread.currentThread().isInterrupted()) {
				// Simulate game tick
				Thread.sleep(100);
				//log.info("GameEngine tick...");
			}
		} catch (InterruptedException e) {
			log.warn("GameEngine interrupted and shut down.");
			Thread.currentThread().interrupt();
		}
	}
}
package com.cosmic.scavengers.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.engine.GameEngine;
import com.cosmic.scavengers.networking.netty.NettyServer;

/**
 * Executes the NettyServer and GameEngine on separate, dedicated threads once
 * the Spring application context has started.
 */
@Component
public class ApplicationStartupRunner implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(ApplicationStartupRunner.class);

	// Using a fixed thread pool of size 2: one for Netty, one for the GameEngine
	private final ExecutorService executorService = Executors.newFixedThreadPool(2);

	private final NettyServer nettyServer;
	private final GameEngine gameEngine;

	// Spring automatically injects the beans you defined with @Component
	public ApplicationStartupRunner(NettyServer nettyServer, GameEngine gameEngine) {
		this.nettyServer = nettyServer;
		this.gameEngine = gameEngine;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Application starting up: Launching NettyServer and GameEngine threads...");
		
		// Start the GameEngine thread
		// This handles the fixed-timestep game loop
		executorService.submit(gameEngine);
		log.info("GameEngine submitted to executor.");

		// Start the NettyServer thread
		// This handles all blocking I/O for the server binding
		executorService.submit(nettyServer);
		log.info("NettyServer submitted to executor.");		
	}
}
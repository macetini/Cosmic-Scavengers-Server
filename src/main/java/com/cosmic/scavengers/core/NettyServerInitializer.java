package com.cosmic.scavengers.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.db.UserRepository;
import com.cosmic.scavengers.db.UserService;
import com.cosmic.scavengers.engine.GameEngine;
import com.cosmic.scavengers.networking.GameChannelHandler;
import com.cosmic.scavengers.networking.NettyServer;

/**
 * The core Spring component that bootstraps the Netty server and the
 * GameEngine. Implements IMessageBroadcaster to allow the GameEngine and
 * GameChannelHandlers to send messages to all connected clients.
 */
@Component
public class NettyServerInitializer implements CommandLineRunner, IMessageBroadcaster {
	private static final Logger log = LoggerFactory.getLogger(NettyServerInitializer.class);

	// --- Connection Management ---
	// Set to hold all active GameChannelHandlers for broadcasting
	private final Set<GameChannelHandler> channelHandlers = Collections.synchronizedSet(new HashSet<>());

	// --- Dependencies Injected by Spring ---
	private final GameEngine engine;
	private final UserRepository userRepository;
	private final UserService userService; // Required for Authentication

	// Constructor Injection: Spring provides the GameEngine, UserRepository, and
	// UserService
	public NettyServerInitializer(GameEngine engine, UserRepository userRepository, UserService userService) {
		this.engine = engine;
		this.userRepository = userRepository;
		this.userService = userService;
	}

	// Executes when the Spring application context is loaded.
	@Override
	public void run(String... args) throws Exception {
		log.info("Starting server components...");

		// 1. Start Game Engine in a background thread
		Thread gameThread = new Thread(engine, "Game-Engine-Thread");
		gameThread.start();
		log.info("GameEngine thread initiated.");

		// 2. Start Netty Server (This call is blocking until the server is shut down)
		// We pass 'this' as the IMessageBroadcaster implementation
		new NettyServer(engine, userRepository, this, userService).run();

		log.info("Server initialization complete.");
	}

	// --- IMessageBroadcaster Implementation ---

	@Override
	public void broadcast(String message, GameChannelHandler sender) {
		// Send the message to all handlers EXCEPT the sender (if specified)
		for (GameChannelHandler handler : channelHandlers) {
			if (handler != sender) {
				handler.sendTextMessage(message);
			}
		}
		log.debug("Broadcasted message: {} (Sender: {})", message, sender != null ? "Client" : "Server");
	}

	// --- Channel Management for IMessageBroadcaster ---

	/**
	 * Called by GameChannelHandler when a channel becomes active.
	 */
	public void addChannelHandler(GameChannelHandler handler) {
		channelHandlers.add(handler);
		log.info("Client added. Total connected clients: {}", channelHandlers.size());
	}

	/**
	 * Called by GameChannelHandler when a channel becomes inactive (disconnected).
	 */
	public void removeChannelHandler(GameChannelHandler handler) {
		channelHandlers.remove(handler);
		log.info("Client removed. Total connected clients: {}", channelHandlers.size());
	}
}
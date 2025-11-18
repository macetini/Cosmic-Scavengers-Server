package com.cosmic.scavengers.networking;

import com.cosmic.scavengers.core.IMessageBroadcaster;
import com.cosmic.scavengers.db.UserRepository;
import com.cosmic.scavengers.db.UserService;
import com.cosmic.scavengers.engine.GameEngine;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * Initializes the Netty Channel Pipeline for new client connections. Sets up
 * decoders, encoders, and injects services into the GameChannelHandler.
 */
public class NettyServerInitializerHandler extends ChannelInitializer<SocketChannel> {

	// Dependencies passed from NettyServer
	private final GameEngine engine;
	private final UserRepository userRepository; // Currently unused, but kept for context
	private final IMessageBroadcaster broadcaster;
	private final UserService userService;

	public NettyServerInitializerHandler(GameEngine engine, UserRepository userRepository,
			IMessageBroadcaster broadcaster, UserService userService) {
		this.engine = engine;
		this.userRepository = userRepository;
		this.broadcaster = broadcaster;
		this.userService = userService;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();

		// Decoders
		// Max frame size: 1MB. Length field is 4 bytes (standard Java int).
		// Length field at offset 0.
		pipeline.addLast("framer", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));		

		// Encoder (Outbound)
		// Adds a 4-byte length prefix (int) to the outbound message.
		pipeline.addLast("prepender", new LengthFieldPrepender(4));		

		// Application Logic Handler
		// Inject the required services into the GameChannelHandler
		pipeline.addLast("handler", new GameChannelHandler(userService, broadcaster));
	}
}
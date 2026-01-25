package com.cosmic.scavengers.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Handles incoming messages from clients, including authentication and game
 * commands. Supports both text-based and binary protocols.
 */
public class GameChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
	private static final Logger log = LoggerFactory.getLogger(GameChannelHandler.class);

	private final CommandRouter commandRouter;

	// Hold a reference to the ChannelHandlerContext so other components can
	// identify the originating channel (useful for broadcasts/exclusions).
	private ChannelHandlerContext ctxRef;

	public GameChannelHandler(CommandRouter networkDispatcher) {
		this.commandRouter = networkDispatcher;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.ctxRef = ctx;
		log.info("Handler added for channel: {}", ctx.channel().remoteAddress());
		super.handlerAdded(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.ctxRef = ctx;
		log.info("Client connected: {}", ctx.channel().remoteAddress());
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("Client disconnected: {}", ctx.channel().remoteAddress());
		super.channelInactive(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		log.info("Read hit on channel {} | Readable bytes: {} | Buffer Hash: {}", ctx.channel().id(),
				msg.readableBytes(), System.identityHashCode(msg));
		if (msg.readableBytes() < 1) {
			log.warn("Received empty message payload.");
			return;
		}
		commandRouter.route(ctx, msg);
	}

	/**
	 * Returns the stored ChannelHandlerContext for this handler. May be null if the
	 * handler hasn't been added yet.
	 */
	public ChannelHandlerContext ctx() {
		return this.ctxRef;
	}
}
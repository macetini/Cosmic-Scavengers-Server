package com.cosmic.scavengers.core.netty;

import com.cosmic.scavengers.networking.GameChannelHandler;
import com.cosmic.scavengers.networking.commands.router.CommandRouter;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {

	private final CommandRouter networkDispatcher;	

	private static final int MAX_FRAME_LENGTH = 1024 * 1024;
	private static final int LENGTH_FIELD_LENGTH = 4;

	public NettyServerInitializer(CommandRouter networkDispatcher) {
		this.networkDispatcher = networkDispatcher;		
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		LengthFieldBasedFrameDecoder decoder = new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, 0,
				LENGTH_FIELD_LENGTH, 0, LENGTH_FIELD_LENGTH);

		final LengthFieldPrepender prepender = new LengthFieldPrepender(LENGTH_FIELD_LENGTH);
		final GameChannelHandler handler = new GameChannelHandler(networkDispatcher);
		ch.pipeline().addLast(decoder, prepender, handler);
	}
}
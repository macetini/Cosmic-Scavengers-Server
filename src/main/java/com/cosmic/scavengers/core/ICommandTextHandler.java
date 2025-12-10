package com.cosmic.scavengers.core;

import com.cosmic.scavengers.networking.NetworkTextCommands;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface ICommandTextHandler {
	/**
	 * Specifies which command this handler is responsible for.
	 */
	NetworkTextCommands getCommand();
	
	void handle(ChannelHandlerContext ctx, ByteBuf payload);
}
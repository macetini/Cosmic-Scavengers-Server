package com.cosmic.scavengers.core;

import com.cosmic.scavengers.networking.NetworkBinaryCommands;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface ICommandBinaryHandler {
	/**
	 * Specifies which command this handler is responsible for.
	 */
	NetworkBinaryCommands getCommand();	
	
	void handle(ChannelHandlerContext ctx, ByteBuf payload);
}
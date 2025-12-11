package com.cosmic.scavengers.core.commands;

import com.cosmic.scavengers.networking.commands.NetworkTextCommands;

import io.netty.channel.ChannelHandlerContext;

public interface ICommandTextHandler {
	/**
	 * Specifies which command this handler is responsible for.
	 */
	NetworkTextCommands getCommand();
	
	void handle(ChannelHandlerContext ctx, String[] payload);
}
package com.cosmic.scavengers.networking.requests.handlers;

import com.cosmic.scavengers.networking.NetworkBinaryCommands;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface ICommandHandler {

	/**
	 * Executes the business logic associated with the command.
	 * 
	 * @param ctx     The Netty context (for sending responses).
	 * @param payload The raw ByteBuf containing the message data.
	 */
	void handle(ChannelHandlerContext ctx, ByteBuf payload);

	/**
	 * Specifies which command this handler is responsible for.
	 */
	NetworkBinaryCommands getCommand();
}

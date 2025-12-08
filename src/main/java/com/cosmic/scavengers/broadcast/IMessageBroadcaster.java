package com.cosmic.scavengers.broadcast;

import com.cosmic.scavengers.networking.GameChannelHandler;

import io.netty.buffer.ByteBuf;

public interface IMessageBroadcaster {
	/**
	 * Broadcasts a message to all connected clients except the original sender.
	 * 
	 * @param message The message content to be sent.
	 * 
	 * @param sender The handler (client connection) that originated the message,
	 *               which should be excluded from the broadcast.
	 */
	void broadcastString(String message, GameChannelHandler sender);

	/**
	 * Broadcasts a binary payload to all connected clients.
	 * 
	 * @param binaryPayload The binary data to be sent.
	 */
	void broadcastBinary(ByteBuf binaryPayload);
}
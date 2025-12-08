package com.cosmic.scavengers.broadcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.networking.GameChannelHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class MessageBroadcasterImpl implements IMessageBroadcaster {
	private static final Logger log = LoggerFactory.getLogger(MessageBroadcasterImpl.class);

	/**
	 * Netty's ChannelGroup is a thread-safe set of Channels. GlobalEventExecutor is
	 * often used as the executor for server-wide tasks.
	 */
	private final ChannelGroup activeChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

	/**
	 * Broadcasts a binary message (ByteBuf) to all currently connected clients.
	 * This is primarily used by the StateBroadcasterImpl to send game state
	 * updates.
	 * 
	 * @param message The binary message buffer to send.
	 */
	@Override
	public void broadcastBinary(ByteBuf message) {
		if (activeChannels.isEmpty()) {
			// log.trace("No active channels to broadcast to."); // Too noisy for trace
			// Release the buffer if no one is listening to prevent memory leak
			message.release();
			return;
		}
		
		ChannelGroupFuture future = activeChannels.writeAndFlush(message.retainedDuplicate());

		future.addListener(f -> {
			if (!f.isSuccess()) {
				log.error("Failed to broadcast message to all clients.", f.cause());
			} else {
				log.trace("Successfully broadcasted {} bytes to {} clients.", message.readableBytes(),
						activeChannels.size());
			}
			message.release();
		});
	}

	/**
	 * Adds a newly active channel to the broadcast group.
	 * 
	 * @param channel The channel to add.
	 */
	public void addChannel(Channel channel) {
		activeChannels.add(channel);
		log.info("Channel added to broadcast group. Total clients: {}", activeChannels.size());
	}

	/**
	 * Removes a closed channel from the broadcast group.
	 * 
	 * @param channel The channel to remove.
	 */
	public void removeChannel(Channel channel) {
		activeChannels.remove(channel);
		log.info("Channel removed from broadcast group. Total clients: {}", activeChannels.size());
	}

	@Override
	public void broadcastString(String message, GameChannelHandler sender) {
		// TODO Auto-generated method stub
		
	}
}
package com.cosmic.scavengers.networking.requests.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.ICommandTextHandler;
import com.cosmic.scavengers.networking.NetworkTextCommands;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

@Component
public class ConnectCommandHandler implements ICommandTextHandler {
	private static final Logger log = LoggerFactory.getLogger(ConnectCommandHandler.class);

	@Override
	public NetworkTextCommands getCommand() {
		return NetworkTextCommands.C_CONNECT;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, ByteBuf payload) {
		log.info("Handling {} command for channel {}.", getCommand().getLogName(), ctx.channel().id());
	}
	
	
}

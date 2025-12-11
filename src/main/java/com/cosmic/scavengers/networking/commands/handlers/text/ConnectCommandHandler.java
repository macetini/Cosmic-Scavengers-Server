package com.cosmic.scavengers.networking.commands.handlers.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandTextHandler;
import com.cosmic.scavengers.networking.commands.NetworkTextCommands;
import com.cosmic.scavengers.networking.commands.sender.MessageSender;

import io.netty.channel.ChannelHandlerContext;

@Component
public class ConnectCommandHandler implements ICommandTextHandler {
	private static final Logger log = LoggerFactory.getLogger(ConnectCommandHandler.class);
	
	private final MessageSender messageSender;
	
	public ConnectCommandHandler(MessageSender messageSender) {
		this.messageSender = messageSender;
	}

	@Override
	public NetworkTextCommands getCommand() {
		return NetworkTextCommands.C_CONNECT;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, String[] parts) {
		log.info("Handling {} text command for channel {}.", getCommand().getLogName(), ctx.channel().id());
		messageSender.sendTextMessage(ctx, NetworkTextCommands.S_CONNECT_OK.getCode());
	}
}

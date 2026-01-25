package com.cosmic.scavengers.networking.handlers.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandTextHandler;
import com.cosmic.scavengers.networking.MessageDispatcher;
import com.cosmic.scavengers.networking.commands.NetworkTextCommand;

import io.netty.channel.ChannelHandlerContext;

@Component
public class ConnectCommandHandler implements ICommandTextHandler {
	private static final Logger log = LoggerFactory.getLogger(ConnectCommandHandler.class);
	
	private final MessageDispatcher messageDispatcher;
	
	public ConnectCommandHandler(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public NetworkTextCommand getCommand() {
		return NetworkTextCommand.C_CONNECT;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, String[] parts) {
		log.info("Handling {} text command for channel {}.", getCommand().getLogName(), ctx.channel().id());
		messageDispatcher.sendTextMessage(ctx, NetworkTextCommand.S_CONNECT_PASS.getCode());
	}
}

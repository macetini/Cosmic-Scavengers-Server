package com.cosmic.scavengers.networking.commands.handlers.text;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandTextHandler;
import com.cosmic.scavengers.db.model.tables.pojos.Players;
import com.cosmic.scavengers.db.services.jooq.UserService;
import com.cosmic.scavengers.networking.commands.NetworkTextCommand;
import com.cosmic.scavengers.networking.commands.dispatcher.MessageDispatcher;

import io.netty.channel.ChannelHandlerContext;

@Component
public class LoginCommandHandler implements ICommandTextHandler {
	private static final Logger log = LoggerFactory.getLogger(LoginCommandHandler.class);

	private final MessageDispatcher messageDispatcher;
	private final UserService userService;

	public LoginCommandHandler(MessageDispatcher messageDispatcher, UserService userService) {
		this.messageDispatcher = messageDispatcher;
		this.userService = userService;
	}

	@Override
	public NetworkTextCommand getCommand() {
		return NetworkTextCommand.C_LOGIN;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, String[] payload) {
		log.info("Handling {} command for channel {}.", getCommand().getLogName(), ctx.channel().id());
		
		if (payload.length < 3) { // Expecting at least 3 parts: COMMAND|username|password
			log.warn("Invalid login payload format from channel {}.", ctx.channel().id());
			messageDispatcher.sendTextMessage(ctx, NetworkTextCommand.S_LOGIN_FAIL + "|INVALID_FORMAT");
			return;
		}
		String username = payload[1];
		String password = payload[2];

		Optional<Players> playerOptional = userService.loginUser(username, password);
		if (playerOptional.isPresent()) {
			Players player = playerOptional.get();
			log.info("Player {} (ID: {}) logged in successfully.", username, player.getId());
			messageDispatcher.sendTextMessage(ctx, NetworkTextCommand.S_LOGIN_PASS + "|" + player.getId());
		} else {
			log.warn("Login failed for user: {}", username);
			messageDispatcher.sendTextMessage(ctx, NetworkTextCommand.S_LOGIN_FAIL + "|INVALID_CREDENTIALS");
		}
	}

}

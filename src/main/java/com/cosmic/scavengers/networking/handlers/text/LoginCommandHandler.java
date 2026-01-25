package com.cosmic.scavengers.networking.handlers.text;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandTextHandler;
import com.cosmic.scavengers.db.model.tables.pojos.Players;
import com.cosmic.scavengers.db.services.UserService;
import com.cosmic.scavengers.networking.MessageDispatcher;
import com.cosmic.scavengers.networking.commands.NetworkTextCommand;
import com.cosmic.scavengers.networking.constants.NetworkAttributeKeys;

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

			Long playerId = player.getId();
			ctx.channel().attr(NetworkAttributeKeys.PLAYER_ID_KEY.getKey()).set(playerId);

			log.info("Player {} (ID: {}) logged in successfully.", username, playerId);
			messageDispatcher.sendTextMessage(ctx, NetworkTextCommand.S_LOGIN_PASS + "|" + playerId);
		} else {
			log.warn("Login failed for user: {}", username);
			messageDispatcher.sendTextMessage(ctx, NetworkTextCommand.S_LOGIN_FAIL + "|INVALID_CREDENTIALS");
		}
	}

}

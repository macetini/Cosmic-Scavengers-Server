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
public class RegisterCommandHandler implements ICommandTextHandler {
	private static final Logger log = LoggerFactory.getLogger(RegisterCommandHandler.class);

	private final MessageDispatcher messageDispatcher;
	private final UserService userService;

	public RegisterCommandHandler(MessageDispatcher messageDispatcher, UserService userService) {
		this.messageDispatcher = messageDispatcher;
		this.userService = userService;
	}

	@Override
	public NetworkTextCommand getCommand() {
		return NetworkTextCommand.C_REGISTER;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, String[] payload) {
		log.info("Handling {} command for channel {}.", getCommand().getLogName(), ctx.channel().id());
		if (payload.length < 3) {
			messageDispatcher.sendTextMessage(ctx, "S_REGISTER_FAIL|INVALID_FORMAT");
			return;
		}
		String username = payload[1];
		String password = payload[2];
		Optional<Players> playerOptional = userService.registerUser(username, password);

		if (playerOptional.isPresent()) {
			Players player = playerOptional.get();
			log.info("Player {} (ID: {}) registered and logged in.", username, player.getId());
			messageDispatcher.sendTextMessage(ctx, "S_REGISTER_OK|" + player.getId());
		} else {
			log.warn("Registration failed for user: {}. Username likely taken.", username);
			messageDispatcher.sendTextMessage(ctx, "S_REGISTER_FAIL|USERNAME_TAKEN");
		}
	}

}

package com.cosmic.scavengers.networking;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.db.meta.Player;
import com.cosmic.scavengers.services.UserService;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * Handles incoming messages from clients, including authentication and game
 * commands. Supports both text-based and binary protocols.
 */
public class GameChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
	private static final Logger log = LoggerFactory.getLogger(GameChannelHandler.class);

	// --- Message Type Constants (Must match client definition) ---
	private static final byte TYPE_TEXT = 0x01;
	private static final byte TYPE_BINARY = 0x02;

	private final UserService userService;

	public GameChannelHandler(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		log.info("Handler added for channel: {}", ctx.channel().remoteAddress());
		super.handlerAdded(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("Client connected: {}", ctx.channel().remoteAddress());

		sendTextMessage(ctx, "S_CONNECT_OK");

		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("Client disconnected: {}", ctx.channel().remoteAddress());
		super.channelInactive(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		if (msg.readableBytes() < 1) {
			log.warn("Received empty message payload.");
			return;
		}

		byte messageType = msg.readByte();

		if (messageType == TYPE_TEXT) {
			String message = msg.toString(CharsetUtil.UTF_8).trim();
			log.info("Received TEXT: {}", message);

			String[] parts = message.split("\\|");
			if (parts.length == 0) {
				log.info(message);
				return;
			}

			String command = parts[0];

			switch (command) {
			case "C_LOGIN":
				// FIXED: Pass ctx to the handler
				handleLogin(ctx, parts);
				break;
			case "C_REGISTER":
				// FIXED: Pass ctx to the handler
				handleRegister(ctx, parts);
				break;
			default:
				log.warn("Unknown text command received: {}", command);
				// FIXED: Pass ctx to the sender
				sendTextMessage(ctx, "S_ERROR|UNKNOWN_COMMAND");
				break;
			}

		} else if (messageType == TYPE_BINARY) {
			// --- BINARY PROTOCOL (Game State) ---

			int payloadSize = msg.readableBytes();

			log.info("Received BINARY payload: {} bytes", payloadSize);
			// Example: handleBinaryGameData(ctx, msg);

		} else {
			log.warn("Received unknown message type: 0x{}", Integer.toHexString(messageType & 0xFF));
		}
	}

	/**
	 * Handles the client login request. FIXED: Now accepts ChannelHandlerContext.
	 */
	private void handleLogin(ChannelHandlerContext ctx, String[] parts) {
		// Protocol check: C_LOGIN|username|password
		if (parts.length < 3) {
			sendTextMessage(ctx, "S_LOGIN_FAIL|INVALID_FORMAT");
			return;
		}

		String username = parts[1];
		String password = parts[2];

		Optional<Player> playerOptional = userService.loginUser(username, password);

		if (playerOptional.isPresent()) {
			Player player = playerOptional.get();
			log.info("Player {} (ID: {}) logged in successfully.", username, player.getId());
			// Success: S_LOGIN_OK|PlayerID
			sendTextMessage(ctx, "S_LOGIN_OK|" + player.getId());
		} else {
			log.warn("Login failed for user: {}", username);
			sendTextMessage(ctx, "S_LOGIN_FAIL|INVALID_CREDENTIALS");
		}
	}

	/**
	 * Handles the client registration request. FIXED: Now accepts
	 * ChannelHandlerContext.
	 */
	private void handleRegister(ChannelHandlerContext ctx, String[] parts) {
		// Protocol check: C_REGISTER|username|password
		if (parts.length < 3) {
			sendTextMessage(ctx, "S_REGISTER_FAIL|INVALID_FORMAT");
			return;
		}

		String username = parts[1];
		String password = parts[2];
		Optional<Player> playerOptional = userService.registerUser(username, password);

		if (playerOptional.isPresent()) {
			Player player = playerOptional.get();
			log.info("Player {} (ID: {}) registered and logged in.", username, player.getId());
			// Success: S_REGISTER_OK|PlayerID
			sendTextMessage(ctx, "S_REGISTER_OK|" + player.getId());
		} else {
			log.warn("Registration failed for user: {}. Username likely taken.", username);
			sendTextMessage(ctx, "S_REGISTER_FAIL|USERNAME_TAKEN");
		}
	}

	/**
	 * Sends a text message back to the client, prepending the TEXT message type
	 * byte. FIXED: Now uses the passed ChannelHandlerContext.
	 */
	public void sendTextMessage(ChannelHandlerContext ctx, String message) {
		if (ctx != null && message != null) {
			ByteBuf messagePayload = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);

			ByteBuf finalPayload = Unpooled.buffer(1 + messagePayload.readableBytes());
			finalPayload.writeByte(TYPE_TEXT);
			finalPayload.writeBytes(messagePayload);

			// Use the passed ctx to ensure the response goes to the correct channel
			ctx.writeAndFlush(finalPayload);
		} else {
			log.warn("Attempted to send text message but context or message was null.");
		}
	}

	/**
	 * Sends a binary message back to the client, prepending the BINARY message type
	 * byte. FIXED: Now uses the passed ChannelHandlerContext.
	 */
	public void sendBinaryMessage(ChannelHandlerContext ctx, ByteBuf payload) {
		if (ctx != null && payload != null) {
			ByteBuf finalPayload = Unpooled.buffer(1 + payload.readableBytes());
			finalPayload.writeByte(TYPE_BINARY);
			finalPayload.writeBytes(payload);

			// Use the passed ctx to ensure the response goes to the correct channel
			ctx.writeAndFlush(finalPayload);
		} else {
			log.warn("Attempted to send binary message but context or message was null.");
		}
	}
}
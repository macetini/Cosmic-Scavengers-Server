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

	private enum PacketType {
		TYPE_TEXT((byte) 0x01), TYPE_BINARY((byte) 0x02);
		
		private final byte value;

		PacketType(byte value) {
			this.value = value;
		}

		public byte getValue() {
			return value;
		}
	}

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
		if (messageType == PacketType.TYPE_TEXT.getValue()) {
			String message = msg.toString(CharsetUtil.UTF_8).trim();
			log.info("Received TEXT: {}", message);

			String[] parts = message.split("\\|");
			if (parts.length == 0) {
				log.info(message);
				return;
			}

			String command = parts[0];
			switch (command) {
			case "C_CONNECT":
				log.info("Client connection handshake received.");
				sendTextMessage(ctx, "S_CONNECT_OK");
				break;
			case "C_LOGIN":
				handleLogin(ctx, parts);
				break;
			case "C_REGISTER":
				handleRegister(ctx, parts);
				break;
			case "C_REQUEST_INITIAL_WORLD_STATE":
				handleInitialWorldStateRequest(ctx, parts);
				break;
			default:
				log.warn("Unknown text command received: {}", command);
				sendTextMessage(ctx, "S_ERROR|UNKNOWN_COMMAND");
				break;
			}
		} else if (messageType == PacketType.TYPE_BINARY.getValue()) {
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
			finalPayload.writeByte(PacketType.TYPE_TEXT.getValue());
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
			finalPayload.writeByte(PacketType.TYPE_BINARY.getValue());
			finalPayload.writeBytes(payload);

			// Use the passed ctx to ensure the response goes to the correct channel
			ctx.writeAndFlush(finalPayload);
		} else {
			log.warn("Attempted to send binary message but context or message was null.");
		}
	}

	/**
	 * Handles the client's request for the initial world state.
	 */
	private void handleInitialWorldStateRequest(ChannelHandlerContext ctx, String[] parts) {
		if (parts.length != 2) {
			sendTextMessage(ctx, "S_ERROR|INVALID_FORMAT");
			return;
		}

		long playerId = Long.parseLong(parts[1]);		
		log.info("Player ID {} requested initial world state.", playerId);
	}

}
package com.cosmic.scavengers.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.core.IMessageBroadcaster;
import com.cosmic.scavengers.db.Player;
import com.cosmic.scavengers.db.UserService;

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

	private final UserService userService;
	private final IMessageBroadcaster broadcaster;

	private ChannelHandlerContext ctx; // Store context for sending messages
	private Player authenticatedPlayer;

	public GameChannelHandler(UserService userService, IMessageBroadcaster broadcaster) {
		this.userService = userService;
		this.broadcaster = broadcaster;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("Client connected: {}", ctx.channel().remoteAddress());

		sendTextMessage("S_CONNECT_OK");

		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("Client disconnected: {}", ctx.channel().remoteAddress());
		super.channelInactive(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

		String protocolPeek = msg.toString(0, Math.min(msg.readableBytes(), 10), CharsetUtil.UTF_8);

		if (protocolPeek.startsWith("C_") || protocolPeek.startsWith("R_")) {
			// TODO: Put in a seperate method to reduce complexity

			// Convert the full buffer content to a string
			String message = msg.toString(CharsetUtil.UTF_8).trim();
			log.debug("Received TEXT: {}", message);

			String[] parts = message.split("\\|");
			if (parts.length == 0) {
				return;
			}

			String command = parts[0];

			switch (command) {
			case "C_LOGIN":
				handleLogin(parts);
				break;
			case "C_REGISTER":
				handleRegister(parts);
				break;			
			default:
				log.warn("Unknown text command received: {}", command);
				sendTextMessage("S_ERROR|UNKNOWN_COMMAND");
				break;
			}
		} else {
			// --- BINARY PROTOCOL (Game State) ---
			log.debug("Received BINARY payload: {} bytes", msg.readableBytes());
		}
	}

	// --- Authentication Handlers ---

	private void handleLogin(String[] parts) {
		// Protocol check: C_LOGIN|username|password
		if (parts.length < 3) {
			sendTextMessage("S_LOGIN_FAIL|INVALID_FORMAT");
			return;
		}

		String username = parts[1];
		String password = parts[2];

		Player player = userService.loginUser(username, password);

		if (player != null) {
			this.authenticatedPlayer = player;
			log.info("Player {} (ID: {}) logged in successfully.", username, player.getId());
			// Success: S_LOGIN_OK|PlayerID
			sendTextMessage("S_LOGIN_OK|" + player.getId());
			// Notify other clients that a player has joined (optional, based on game)
			// broadcaster.broadcast("P_JOIN|" + player.getId() + "|" +
			// player.getUsername(), null);
		} else {
			log.warn("Login failed for user: {}", username);
			sendTextMessage("S_LOGIN_FAIL|INVALID_CREDENTIALS");
		}
	}

	private void handleRegister(String[] parts) {
		// Protocol check: C_REGISTER|username|password
		if (parts.length < 3) {
			sendTextMessage("S_REGISTER_FAIL|INVALID_FORMAT");
			return;
		}

		// Extract username and password
		String username = parts[1];
		String password = parts[2];

		Player player = userService.registerUser(username, password);

		if (player != null) {
			this.authenticatedPlayer = player;
			log.info("Player {} (ID: {}) registered and logged in.", username, player.getId());
			// Success: S_REGISTER_OK|PlayerID
			sendTextMessage("S_REGISTER_OK|" + player.getId());
		} else {
			log.warn("Registration failed for user: {}. Username likely taken.", username);
			sendTextMessage("S_REGISTER_FAIL|USERNAME_TAKEN");
		}
	}

	// --- Utility Methods (TODO Maybe put in separate method later on) ---

	public void sendTextMessage(String message) {
		if (ctx != null && message != null) {
			// Write the text content as a ByteBuf
			ByteBuf payload = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);

			// The prepender will automatically add the length prefix.
			ctx.writeAndFlush(payload);
		} else {
			log.warn("Attempted to send text message but context or message was null.");
		}
	}

	public void sendBinaryMessage(ByteBuf payload) {
		if (ctx != null && payload != null) {
			// The prepender will automatically add the length prefix.
			ctx.writeAndFlush(payload);
		} else {
			log.warn("Attempted to send binary message but context or message was null.");
		}
	}
}
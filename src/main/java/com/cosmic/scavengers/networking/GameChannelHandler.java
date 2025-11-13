package com.cosmic.scavengers.networking;

import com.cosmic.scavengers.core.IMessageBroadcaster;
import com.cosmic.scavengers.db.Player;
import com.cosmic.scavengers.db.UserService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty handler that manages a single client connection and processes game
 * commands. NOTE: The type argument must be 'String' because of the
 * StringDecoder in the pipeline.
 */
public class GameChannelHandler extends SimpleChannelInboundHandler<String> {
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
		sendMessage("S_CONNECT_OK");
		// NOTE: Connection tracking (addChannelHandler) needs to be implemented
		// via the IMessageBroadcaster or a direct reference to NettyServerInitializer
		// if you want real-time user count tracking.
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("Client disconnected: {}", ctx.channel().remoteAddress());
		// NOTE: Remove from connection tracking here.
		super.channelInactive(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		// Trim any trailing whitespace/newlines that decoders might miss
		String message = msg.trim();
		log.debug("Received: {}", message);

		String[] parts = message.split("\\|");
		if (parts.length == 0)
			return;

		String command = parts[0];

		switch (command) {
		case "C_LOGIN":
			handleLogin(parts);
			break;
		case "C_REGISTER":
			handleRegister(parts);
			break;
		case "C_MOVE":
			// Process game commands only if authenticated
			if (authenticatedPlayer != null) {
				broadcaster.broadcast(message, this);
			} else {
				sendMessage("S_AUTH_REQUIRED");
			}
			break;
		default:
			log.warn("Unknown command received: {}", command);
			sendMessage("S_ERROR|UNKNOWN_COMMAND");
			break;
		}
	}

	// --- Authentication Handlers ---

	private void handleLogin(String[] parts) {
		// Protocol check: C_LOGIN|username|password
		if (parts.length < 3) {
			sendMessage("S_LOGIN_FAIL|INVALID_FORMAT");
			return;
		}

		String username = parts[1];
		String password = parts[2];

		Player player = userService.loginUser(username, password);

		if (player != null) {
			this.authenticatedPlayer = player;
			log.info("Player {} (ID: {}) logged in successfully.", username, player.getId());
			// Success: S_LOGIN_OK|PlayerID
			sendMessage("S_LOGIN_OK|" + player.getId());
			// Notify other clients that a player has joined (optional, based on game)
			// broadcaster.broadcast("P_JOIN|" + player.getId() + "|" +
			// player.getUsername(), null);
		} else {
			log.warn("Login failed for user: {}", username);
			sendMessage("S_LOGIN_FAIL|INVALID_CREDENTIALS");
		}
	}

	private void handleRegister(String[] parts) {
		// Protocol check: C_REGISTER|username|password
		if (parts.length < 3) {
			sendMessage("S_REGISTER_FAIL|INVALID_FORMAT");
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
			sendMessage("S_REGISTER_OK|" + player.getId());
		} else {
			log.warn("Registration failed for user: {}. Username likely taken.", username);
			sendMessage("S_REGISTER_FAIL|USERNAME_TAKEN");
		}
	}

	// --- Utility Method ---

	public void sendMessage(String message) {
		if (ctx != null && message != null) {
			// Sends the string followed by a newline, which is essential for the
			// LineBasedFrameDecoder on the client/server side to frame the message.
			ctx.writeAndFlush(message + "\n");
		} else {
			log.warn("Attempted to send message but context or message was null.");
		}
	}
}
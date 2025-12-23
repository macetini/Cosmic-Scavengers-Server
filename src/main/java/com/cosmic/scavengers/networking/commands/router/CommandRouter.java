package com.cosmic.scavengers.networking.commands.router;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandBinaryHandler;
import com.cosmic.scavengers.core.commands.ICommandTextHandler;
import com.cosmic.scavengers.networking.commands.NetworkBinaryCommand;
import com.cosmic.scavengers.networking.commands.NetworkTextCommand;
import com.cosmic.scavengers.networking.commands.router.meta.CommandType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import jakarta.annotation.PostConstruct;

/**
 * This component replaces the central 'switch' statement. It maps command codes
 * to their dedicated handler classes.
 */
@Component
public class CommandRouter {
	private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);

	private Map<NetworkBinaryCommand, ICommandBinaryHandler> binaryCommandsMap;
	private final List<ICommandBinaryHandler> binaryHandlers;

	private Map<NetworkTextCommand, ICommandTextHandler> textCommandsMap;
	private final List<ICommandTextHandler> textHandlers;

	public CommandRouter(List<ICommandBinaryHandler> binaryCommands, List<ICommandTextHandler> textCommands) {
		this.binaryHandlers = binaryCommands;
		this.textHandlers = textCommands;
	}

	/**
	 * Initializes the command map after all handlers have been injected.
	 */
	@PostConstruct
	public void init() {
		binaryCommandsMap = binaryHandlers.stream()
				.collect(Collectors.toMap(ICommandBinaryHandler::getCommand, Function.identity()));
		log.info("Initialized Network Command Router with {} Binary handlers.", binaryCommandsMap.size());

		textCommandsMap = textHandlers.stream()
				.collect(Collectors.toMap(ICommandTextHandler::getCommand, Function.identity()));
		log.info("Initialized Network Command Router with {} Text handlers.", textCommandsMap.size());
	}

	/**
	 * 
	 * Executes the appropriate handler for the incoming command. This method is
	 * called from your Netty ChannelInboundHandler.
	 * 
	 * @param commandCode The command identifier read from the header.
	 * @param ctx         The channel context.
	 * @param payload     The message payload.
	 * 
	 */
	public void route(ChannelHandlerContext ctx, ByteBuf command) {
		//TODO: OCP violation here, needs refactor.
		byte commandType = command.readByte();
		if (commandType == CommandType.TYPE_TEXT.getValue()) {
			routeTextCommand(ctx, command);
		} else if (commandType == CommandType.TYPE_BINARY.getValue()) {
			routeBinaryCommand(ctx, command);
		} else {
			if (log.isWarnEnabled()) {
				log.warn("Received unknown message type: 0x{}", Integer.toHexString(commandType & 0xFF));
			}
		}
	}

	private void routeTextCommand(ChannelHandlerContext ctx, ByteBuf payload) {
		String message = payload.toString(CharsetUtil.UTF_8).trim();
		String[] parts = message.split("\\|");
		if (parts.length == 0) {
			log.warn("Received empty text command.");
			return;
		}
		String commandCode = parts[0];
		NetworkTextCommand command = NetworkTextCommand.fromCode(commandCode);

		if (command == null) {
			log.warn("Received unknown text command code: '{}'. Dropping payload.", commandCode);
			payload.release();
			return;
		}

		log.info("Routing text command: {}", command.getLogName());
		ICommandTextHandler handler = textCommandsMap.get(command);

		if (handler != null) {
			handler.handle(ctx, parts);
		} else {
			log.warn("No text handler implemented for command: {}", command.getLogName());
			payload.release();
		}
	}

	private void routeBinaryCommand(ChannelHandlerContext ctx, ByteBuf payload) {
		if (payload.readableBytes() < 2) {
			log.warn("Binary payload too short to contain command.");
			return;
		}

		short commandCode = payload.readShort();
		NetworkBinaryCommand command = NetworkBinaryCommand.fromCode(commandCode);
		if (command == null) {
			if (log.isWarnEnabled()) { // Check if WARN is enabled before performing HexString conversion
				log.warn("Received unknown command code: 0x{}. Dropping payload.",
						Integer.toHexString(commandCode & 0xFFFF));
			}
			payload.release();
			return;
		}

		ICommandBinaryHandler handler = binaryCommandsMap.get(command);
		log.info("Routing binary command: {}", command.getLogText());

		if (handler != null) {
			handler.handle(ctx, payload);
		} else {
			log.warn("No handler implemented for command: {}", command.getLogText());
			payload.release();
		}
	}
}
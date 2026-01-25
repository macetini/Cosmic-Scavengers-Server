package com.cosmic.scavengers.networking;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandBinaryHandler;
import com.cosmic.scavengers.core.commands.ICommandTextHandler;
import com.cosmic.scavengers.networking.commands.CommandType;
import com.cosmic.scavengers.networking.commands.NetworkBinaryCommand;
import com.cosmic.scavengers.networking.commands.NetworkTextCommand;

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

	private static final String TEXT_COMMAND_DELIMITER = "\\|";

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
	 * Routes a command to the correct handler.
	 * 
	 * @param ctx     The Netty ChannelHandlerContext.
	 * @param command The Command Payload.
	 * 
	 */
	public void route(ChannelHandlerContext ctx, ByteBuf command) {
		byte commandValue = command.readByte();
		CommandType commandType = CommandType.fromValue(commandValue);
		switch (commandType) {
		case TYPE_TEXT:
			routeTextCommand(ctx, command);
			break;
		case TYPE_BINARY:
			routeBinaryCommand(ctx, command);
			break;
		case TYPE_UNKNOWN:
			log.warn("Received unknown message type: {}", commandType);
			break;
		default:
			throw new IllegalStateException("Unexpected value: " + commandType);
		}
	}

	private void routeTextCommand(ChannelHandlerContext ctx, ByteBuf payload) {
		String message = payload.toString(CharsetUtil.UTF_8).trim();
		String[] parts = message.split(TEXT_COMMAND_DELIMITER);
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
			log.warn("Binary Payload too short to contain command.");
			return;
		}

		short commandCode = payload.readShort();
		NetworkBinaryCommand command = NetworkBinaryCommand.fromCode(commandCode);
		if (command == null) {
			if (log.isWarnEnabled()) { // Check if WARN is enabled before performing HexString conversion
				log.warn("Received unknown command code: '0x{}'. Dropping payload.",
						Integer.toHexString(commandCode & 0xFFFF));
			}
			payload.release(); // TODO - Check if this done automatically.
			return;
		}

		ICommandBinaryHandler handler = binaryCommandsMap.get(command);
		log.info("Routing [Inbound BINARY] Command | Log: [{}]", command.getLogText());

		if (handler != null) {
			handler.handle(ctx, payload);
		} else {
			log.warn("No Handler implemented for [Inbound Command] | Log: [{}]", command.getLogText());
			payload.release(); // TODO - Check if this done automatically.
		}
	}
}
package com.cosmic.scavengers.networking;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.networking.requests.handlers.ICommandHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.PostConstruct;

/**
 * This component replaces the central 'switch' statement. It maps command codes
 * to their dedicated handler classes.
 */
@Component
public class NetworkDispatcher {
	private static final Logger log = LoggerFactory.getLogger(NetworkDispatcher.class);

	private Map<NetworkBinaryCommands, ICommandHandler> commandMap;

	private final List<ICommandHandler> handlers;

	public NetworkDispatcher(List<ICommandHandler> handlers) {
		this.handlers = handlers;
	}

	/**
	 * Initializes the command map after all handlers have been injected.
	 */
	@PostConstruct
	public void init() {
		this.commandMap = handlers.stream().collect(Collectors.toMap(ICommandHandler::getCommand, Function.identity()));
		log.info("Initialized command dispatcher with {} handlers.", this.commandMap.size());
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
	public void dispatch(short commandCode, ChannelHandlerContext ctx, ByteBuf payload) {
		NetworkBinaryCommands command = NetworkBinaryCommands.fromCode(commandCode);

		if (command == null) {
			log.warn("Received unknown command code: 0x{}. Dropping payload.",
					Integer.toHexString(commandCode & 0xFFFF));
			payload.release();
			return;
		}

		ICommandHandler handler = commandMap.get(command);

		if (handler != null) {
			// EXECUTION: Looks up the handler and calls its method
			handler.handle(ctx, payload);
		} else {
			log.warn("No handler implemented for command: {}", command.getLogName());
			payload.release();
		}
	}
}
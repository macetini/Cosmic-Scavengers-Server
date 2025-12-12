package com.cosmic.scavengers.networking.commands.sender;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.networking.commands.router.meta.CommandType;
import com.google.protobuf.GeneratedMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * Service component responsible for serializing and sending messages
 * (responses) back to the client over a Netty channel, abstracting the
 * low-level protocol structure.
 */
@Component
public class MessageSender {
	private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

	public void sendTextMessage(ChannelHandlerContext ctx, String message) {
		if (ctx == null || message == null) {
			log.warn("Attempted to send text message but context or message was null.");
			return;
		}

		final ByteBuf messagePayload = Unpooled.copiedBuffer(message, StandardCharsets.UTF_8);

		// 1 Byte (Type) + N Bytes (Payload)
		final ByteBuf finalPayload = Unpooled.buffer(Byte.BYTES + messagePayload.readableBytes());

		finalPayload.writeByte(CommandType.TYPE_TEXT.getValue()); // 1 Byte: Protocol Type (0x01)
		finalPayload.writeBytes(messagePayload); // N bytes: Actual Text Payload

		messagePayload.release();

		log.info("Sending TEXT message '{}' - size '{}' bytes", message, finalPayload.readableBytes());
		ctx.writeAndFlush(finalPayload);
	}

	/**
	 * Serializes and sends a Protocol Buffers message back to the client as a
	 * binary message.
	 * 
	 * @param ctx     The channel context to write the response to.
	 * @param message The Protocol Buffers message to serialize and send.
	 * @param command The command code (short) being sent back.
	 * 
	 */
	public void sendBinaryProtbufMessage(ChannelHandlerContext ctx, GeneratedMessage message, short command) {
		if (ctx == null || message == null) {
			log.warn("Attempted to send protobuf binary message but context or message was null.");
			return;
		}

		byte[] serializedBytes = message.toByteArray();
		ByteBuf serializedPayload = Unpooled.buffer(serializedBytes.length);
		serializedPayload.writeBytes(serializedBytes);

		sendBinaryMessage(ctx, serializedPayload, command);
	}

	/**
	 * Sends a binary message back to the client, handling the low-level header
	 * structure (Type, Command, Length). * @param ctx The channel context to write
	 * the response to.
	 * 
	 * @param payload The binary message content (will be released by this method).
	 * @param command The command code (short) being sent back.
	 */
	public void sendBinaryMessage(ChannelHandlerContext ctx, ByteBuf payload, short command) {
		if (ctx == null || payload == null) {
			if (payload != null) {
				payload.release();
			}
			log.warn("Attempted to send binary message but context or payload was null.");
			return;
		}

		// Header size: 1 (Type) + 2 (Command) + 4 (Length) = 7 bytes
		final int headerSize = Byte.BYTES + Short.BYTES + Integer.BYTES;

		// Payload size: N bytes
		final int payloadSize = payload.readableBytes();

		// Total size = Header + Payload
		final int totalSize = headerSize + payloadSize;

		final ByteBuf finalPayload = Unpooled.buffer(totalSize); // Final buffer

		// Write Header
		finalPayload.writeByte(CommandType.TYPE_BINARY.getValue()); // 1 Byte: Protocol Type (0x02)
		finalPayload.writeShort(command); // 2 Byte: Command
		finalPayload.writeInt(payloadSize); // 4 Byte: Payload Length N

		// Write N bytes Payload
		finalPayload.writeBytes(payload);

		payload.release(); // Release the original/old payload buffer

		log.info("Sending BINARY command '0x{}' - size '{}' bytes", finalPayload.readableBytes(), payloadSize);

		ctx.writeAndFlush(finalPayload);
	}
}
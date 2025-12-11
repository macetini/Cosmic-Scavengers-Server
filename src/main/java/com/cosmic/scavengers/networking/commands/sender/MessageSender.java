package com.cosmic.scavengers.networking.commands.sender;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.networking.commands.router.meta.CommandType;

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

		ByteBuf messagePayload = Unpooled.copiedBuffer(message, StandardCharsets.UTF_8);

		// 1 Byte (Type) + N Bytes (Payload)
		ByteBuf finalPayload = Unpooled.buffer(Byte.BYTES + messagePayload.readableBytes());

		finalPayload.writeByte(CommandType.TYPE_TEXT.getValue()); // 1 Byte: Protocol Type (0x01)
		finalPayload.writeBytes(messagePayload); // N bytes: Actual Text Payload

		messagePayload.release();

		log.info("Sending TEXT message '{}' - size '{}' bytes", message, finalPayload.readableBytes());
		ctx.writeAndFlush(finalPayload);
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
			if (payload != null)
				payload.release(); // Ensure we don't leak if only payload is present
			log.warn("Attempted to send binary message but context or payload was null.");
			return;
		}

		// Calculate total required buffer size
		// Header size: 1 (Type) + 2 (Command) + 4 (Length) = 7 bytes
		final int HEADER_SIZE = Byte.BYTES + Short.BYTES + Integer.BYTES;

		int payloadSize = payload.readableBytes();
		int totalSize = HEADER_SIZE + payloadSize;

		// Create the final buffer
		ByteBuf finalPayload = Unpooled.buffer(totalSize);

		// 1. Write Header
		finalPayload.writeByte(CommandType.TYPE_BINARY.getValue()); // 1 Byte: Protocol Type (0x02)
		finalPayload.writeShort(command); // 2 Byte: Command
		finalPayload.writeInt(payloadSize); // 4 Byte: Payload Length N

		// 2. Write Payload
		finalPayload.writeBytes(payload); // N bytes: Actual Payload

		// CRITICAL: Release the original payload buffer, as its data has been copied
		// into finalPayload.
		payload.release();

		log.info("Sending BINARY command 0x{} of size {} bytes (payload: {}).", Integer.toHexString(command & 0xFFFF),
				finalPayload.readableBytes(), payloadSize);

		// Use the passed ctx to ensure the response goes to the correct channel
		ctx.writeAndFlush(finalPayload);

		// Note: finalPayload is managed by Netty after writeAndFlush completes.
	}
}
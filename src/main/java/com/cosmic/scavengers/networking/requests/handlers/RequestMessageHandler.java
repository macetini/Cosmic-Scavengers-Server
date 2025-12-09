package com.cosmic.scavengers.networking.requests.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.networking.requests.handlers.meta.PacketType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

public class RequestMessageHandler {
	private static final Logger log = LoggerFactory.getLogger(RequestMessageHandler.class);

	private final ChannelHandlerContext ctx;

	public RequestMessageHandler(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * Sends a text message back to the client, prepending the TEXT message type
	 * byte. FIXED: Now uses the passed ChannelHandlerContext.
	 */
	public void sendTextMessage(String message) {
		if (ctx != null && message != null) {
			ByteBuf messagePayload = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);

			ByteBuf finalPayload = Unpooled.buffer(1 + messagePayload.readableBytes());
			finalPayload.writeByte(PacketType.TYPE_TEXT.getValue());
			finalPayload.writeBytes(messagePayload);

			log.info("Sending Text message '{}' of size '{}' bytes.", message, finalPayload.readableBytes());
			ctx.writeAndFlush(finalPayload);
		} else {
			log.warn("Attempted to send text message but context or message was null.");
		}
	}

	/**
	 * Sends a binary message back to the client, prepending the BINARY message type
	 * byte and command. FIXED: Now uses the passed ChannelHandlerContext.
	 */
	public void sendBinaryMessage(ByteBuf payload, short command) {
		if (ctx == null || payload == null) {
			if (payload != null)
				payload.release();
			log.warn("Attempted to send binary message but context or message was null.");
			return;
		}

		// Header size: 1 (Type) + 2 (Command) + 4 (Length) = 7 bytes
		int bufferLength = Byte.BYTES + Short.BYTES + Integer.BYTES;

		int payloadSize = payload.readableBytes();
		bufferLength += payloadSize;

		ByteBuf finalPayload = Unpooled.buffer(bufferLength);

		finalPayload.writeByte(PacketType.TYPE_BINARY.getValue()); // 1 Byte: Protocol Type
		finalPayload.writeShort(command); // 2 Byte: Command (Little Endian)
		finalPayload.writeInt(payloadSize); // 4 Byte: Payload Length N
		finalPayload.writeBytes(payload); // N bytes: Actual Payload

		payload.release();

		// Use the passed ctx to ensure the response goes to the correct channel
		log.info("Sending Binry message of size '{}' bytes.", finalPayload.readableBytes());
		ctx.writeAndFlush(finalPayload);
	}

}

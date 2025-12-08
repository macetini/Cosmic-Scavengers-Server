package com.cosmic.scavengers.networking.netty;

import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Utility for handling final network message framing and sending using Netty's ByteBuf.
 * This ensures the message conforms to the custom protocol: 
 * [4-byte Length (Big-Endian)] + [1-byte Type] + [Payload]
 */
public class NettyResponseFramer {

    private static final Logger log = LoggerFactory.getLogger(NettyResponseFramer.class);

    /**
     * Takes the serialized payload and wraps it in the final network frame, then sends it.
     * * @param ctx The Netty ChannelHandlerContext.
     * @param payload The raw binary data (e.g., serialized entities).
     * @param messageType The 1-byte message type (e.g., S_WORLD_STATE_DATA).
     */
    public static void sendFramedResponse(ChannelHandlerContext ctx, byte[] payload, byte messageType) {
        // Payload length = 1 (Type byte) + Data length
        int payloadLength = 1 + payload.length;

        // Total frame length = 4 (Length prefix) + Payload length
        int totalFrameLength = 4 + payloadLength;
        
        // 1. Allocate buffer using the context's allocator (recommended Netty practice)
        // We set the ByteOrder to BIG_ENDIAN for the Length Prefix, as the client reverses it for compatibility.
        ByteBuf buffer = ctx.alloc().buffer(totalFrameLength).order(ByteOrder.BIG_ENDIAN);

        // --- 1. Write 4-byte Length Prefix (Big-Endian) ---
        buffer.writeInt(payloadLength);
        
        // --- 2. Write 1-byte Message Type ---
        // Since this is a single byte, endianness doesn't matter, but it is written after the length.
        buffer.writeByte(messageType);
        
        // --- 3. Write the Payload Data ---
        // Write the pre-serialized entity data.
        buffer.writeBytes(payload);
        
        // Send the complete framed ByteBuf
        ctx.writeAndFlush(buffer);
        log.debug("Sent framed binary response (Type 0x{}) of total length {} bytes.", 
                  Integer.toHexString(messageType & 0xFF), totalFrameLength);
    }

    /**
     * Alternative method to convert a raw byte array (produced by a handler) into a ByteBuf
     * for sending, if the handler already includes the framing.
     */
    public static void sendRawBuffer(ChannelHandlerContext ctx, byte[] framedBuffer) {
         ByteBuf buffer = ctx.alloc().buffer(framedBuffer.length);
         buffer.writeBytes(framedBuffer);
         ctx.writeAndFlush(buffer);
    }
}
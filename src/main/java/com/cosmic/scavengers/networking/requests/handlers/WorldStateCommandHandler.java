package com.cosmic.scavengers.networking.requests.handlers;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.db.model.tables.pojos.Worlds;
import com.cosmic.scavengers.db.repository.jooq.JooqWorldRepository;
import com.cosmic.scavengers.networking.NetworkBinaryCommands;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

@Component
public class WorldStateCommandHandler implements ICommandHandler {
	private static final Logger log = LoggerFactory.getLogger(WorldStateCommandHandler.class);

	private final JooqWorldRepository worldRepository;

	/**
	 * Constructor injection for dependencies.
	 */
	public WorldStateCommandHandler(JooqWorldRepository worldRepository) {
		this.worldRepository = worldRepository;
	}

	@Override
	public NetworkBinaryCommands getCommand() {
		return NetworkBinaryCommands.REQUEST_WORLD_STATE;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, ByteBuf payload) {
		payload.release();
		log.info("Handling {} command for channel {}.", getCommand().getLogName(), ctx.channel().id());
	}

	public static ByteBuf serializeWorldStateData(Worlds worldData) {
		if (worldData == null) {
			throw new IllegalArgumentException("WorldData cannot be null.");
		}

		int estimatedSize = 0;
		estimatedSize += Long.BYTES; // World ID (long)
		estimatedSize += Integer.BYTES; // World Name Length Prefix (int)
		estimatedSize += worldData.getWorldName().getBytes(StandardCharsets.UTF_8).length; // World Name (UTF-8 chars, 1
																							// bytes each)
		estimatedSize += Long.BYTES; // Map Seed (long)
		estimatedSize += Integer.BYTES; // Sector Size (int)

		ByteBuf buffer = Unpooled.buffer(estimatedSize);

		// 8 bytes: World ID (long)
		buffer.writeLong(worldData.getId());

		// Variable Length String: World Name (Length Prefix)
		byte[] nameBytes = worldData.getWorldName().getBytes(StandardCharsets.UTF_8);
		buffer.writeInt(nameBytes.length); // 4 bytes for length
		buffer.writeBytes(nameBytes); // N bytes for data
		// 8 bytes: Map Seed (long)
		buffer.writeLong(worldData.getMapSeed());
		// 4 bytes: Sector Size (int)
		buffer.writeInt(worldData.getSectorSizeUnits());

		log.info("Serialized world state payload size: {} bytes.", buffer.readableBytes());
		return buffer;
	}
}

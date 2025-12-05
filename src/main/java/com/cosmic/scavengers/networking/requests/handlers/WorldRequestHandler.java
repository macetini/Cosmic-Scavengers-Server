package com.cosmic.scavengers.networking.requests.handlers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.networking.meta.PlayerEntityDTO;
import com.cosmic.scavengers.networking.meta.WorldDataDTO;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class WorldRequestHandler {
	private static final Logger log = LoggerFactory.getLogger(WorldRequestHandler.class);

	public static ByteBuf serializeWorldStateData(WorldDataDTO worldData) {
		if (worldData == null) {
			throw new IllegalArgumentException("WorldData cannot be null.");
		}

		int estimatedSize = 0;
		estimatedSize += Long.BYTES; // World ID (long)
		estimatedSize += Integer.BYTES; // World Name Length Prefix (int)
		estimatedSize += worldData.worldName().getBytes(StandardCharsets.UTF_8).length; // World Name (UTF-8 chars, 1 //
																						// bytes each)
		estimatedSize += Long.BYTES; // Map Seed (long)
		estimatedSize += Integer.BYTES; // Sector Size (int)

		ByteBuf buffer = Unpooled.buffer(estimatedSize);

		// 8 bytes: World ID (long)
		buffer.writeLong(worldData.id());

		// Variable Length String: World Name (Length Prefix)
		byte[] nameBytes = worldData.worldName().getBytes(StandardCharsets.UTF_8);
		buffer.writeInt(nameBytes.length); // 4 bytes for length
		buffer.writeBytes(nameBytes); // N bytes for data
		// 8 bytes: Map Seed (long)
		buffer.writeLong(worldData.mapSeed());
		// 4 bytes: Sector Size (int)
		buffer.writeInt(worldData.sectorSizeUnits());

		log.info("Serialized world state payload size: {} bytes.", buffer.readableBytes());
		return buffer;
	}

	public static ByteBuf serializePlayerEntities(List<PlayerEntityDTO> entities) {
		if (entities == null) {
			throw new IllegalArgumentException("Player entities list cannot be null.");
		}

		int estimatedSize = 0;
		estimatedSize += Integer.BYTES; // Number of Entities (int)

		int numEntities = entities.size();
		int fixedSize = Long.BYTES + // id
				Long.BYTES + // playerId
				Long.BYTES + // worldId

				Integer.BYTES + // chunkX
				Integer.BYTES + // chunkY

				Float.BYTES + // posX
				Float.BYTES + // posY

				Integer.BYTES; // health

		estimatedSize += numEntities * fixedSize;

		ByteBuf buffer = Unpooled.buffer(estimatedSize);

		buffer.writeInt(numEntities);

		for (int i = 0; i < numEntities; i++) {
			PlayerEntityDTO entity = entities.get(i);

			buffer.writeLong(entity.id());
			buffer.writeLong(entity.playerId());
			buffer.writeLong(entity.worldId());

			buffer.writeInt(entity.chunkX());
			buffer.writeInt(entity.chunkY());

			buffer.writeFloat(entity.posX());
			buffer.writeFloat(entity.posY());

			buffer.writeInt(entity.health());
		}

		log.info("Serialized player entities payload size: {} bytes for {} entities.", buffer.readableBytes(),
				numEntities);

		return buffer;
	}
}

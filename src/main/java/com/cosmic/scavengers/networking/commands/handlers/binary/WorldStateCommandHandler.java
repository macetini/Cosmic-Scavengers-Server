package com.cosmic.scavengers.networking.commands.handlers.binary;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandBinaryHandler;
import com.cosmic.scavengers.db.model.tables.pojos.Worlds;
import com.cosmic.scavengers.db.services.jooq.PlayerInitService;
import com.cosmic.scavengers.networking.commands.NetworkBinaryCommands;
import com.cosmic.scavengers.networking.commands.sender.MessageSender;

import cosmic.scavengers.generated.WorldDataOuterClass.WorldData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

@Component
public class WorldStateCommandHandler implements ICommandBinaryHandler {
	private static final Logger log = LoggerFactory.getLogger(WorldStateCommandHandler.class);

	private final MessageSender messageSender;
	private final PlayerInitService playerInitService;

	public WorldStateCommandHandler(MessageSender messageSender, PlayerInitService playerInitService) {
		this.messageSender = messageSender;
		this.playerInitService = playerInitService;
	}

	@Override
	public NetworkBinaryCommands getCommand() {
		return NetworkBinaryCommands.REQUEST_WORLD_STATE_C;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, ByteBuf payload) {
		log.info("Handling {} command for channel {}.", getCommand().getLogName(), ctx.channel().id());

		Long playerId = payload.readLong();
		
		log.info("Fetching world state for player ID: {}", playerId);
		Worlds worlds = playerInitService.getCurrentWorldDataByPlayerId(playerId);
		
		WorldData worldData = WorldData.newBuilder()
				.setId(worlds.getId())
				.setWorldName(worlds.getWorldName())
				.setMapSeed(worlds.getMapSeed())
				.setSectorSizeUnits(worlds.getSectorSizeUnits())
				.setGenerationConfigJson(worlds.getGenerationConfig().data())
				.build();
				
		messageSender.sendBinaryProtbufMessage(ctx, worldData, NetworkBinaryCommands.REQUEST_WORLD_STATE_S.getCode());		
	}

	private ByteBuf serializeWorldStateData(Worlds worldData) {
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

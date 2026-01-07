package com.cosmic.scavengers.networking.commands.handlers.binary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandBinaryHandler;
import com.cosmic.scavengers.db.model.tables.pojos.Worlds;
import com.cosmic.scavengers.db.services.jooq.PlayerInitService;
import com.cosmic.scavengers.networking.commands.NetworkBinaryCommand;
import com.cosmic.scavengers.networking.commands.dispatcher.MessageDispatcher;
import com.cosmicscavengers.networking.protobuf.worlddata.WorldDataOuterClass.WorldData;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

@Component
public class WorldStateCommandHandler implements ICommandBinaryHandler {
	private static final Logger log = LoggerFactory.getLogger(WorldStateCommandHandler.class);

	private final MessageDispatcher messageDispatcher;
	private final PlayerInitService playerInitService;

	public WorldStateCommandHandler(MessageDispatcher messageDispatcher, PlayerInitService playerInitService) {
		this.messageDispatcher = messageDispatcher;
		this.playerInitService = playerInitService;
	}

	@Override
	public NetworkBinaryCommand getCommand() {
		return NetworkBinaryCommand.REQUEST_WORLD_STATE_C;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, ByteBuf payload) {
		log.info("Handling {} command for channel {}.", getCommand().getLogText(), ctx.channel().id());

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
				
		messageDispatcher.sendBinaryProtobufMessage(ctx, worldData, NetworkBinaryCommand.REQUEST_WORLD_STATE_S.getCode());		
	}
}

package com.cosmic.scavengers.networking.commands.handlers.binary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandBinaryHandler;
import com.cosmic.scavengers.db.model.tables.pojos.PlayerEntities;
import com.cosmic.scavengers.db.services.jooq.PlayerInitService;
import com.cosmic.scavengers.networking.commands.NetworkBinaryCommands;
import com.cosmic.scavengers.networking.commands.sender.MessageSender;
import com.cosmic.scavengers.utils.protobuf.ProtobufJsonbUtil;
import com.cosmic.scavengers.utils.protobuf.ProtobufTimeUtil;

import CosmicScavengers.Networking.Protobuf.PlayerEntities.PlayerEntityDataOuterClass.PlayerEntityData;
import CosmicScavengers.Networking.Protobuf.PlayerEntities.PlayerEntityListDataOuterClass.PlayerEntityListData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

@Component
public class PlayerEntitiesCommandHandler implements ICommandBinaryHandler {
	private static final Logger log = LoggerFactory.getLogger(PlayerEntitiesCommandHandler.class);

	private final MessageSender messageSender;
	private final PlayerInitService playerInitService;

	public PlayerEntitiesCommandHandler(MessageSender messageSender, PlayerInitService playerInitService) {
		this.messageSender = messageSender;
		this.playerInitService = playerInitService;
	}

	@Override
	public NetworkBinaryCommands getCommand() {
		return NetworkBinaryCommands.REQUEST_PLAYER_ENTITIES_C;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, ByteBuf payload) {
		log.info("Handling {} command for channel {}.", getCommand().getLogName(), ctx.channel().id());

		Long playerId = payload.readLong();
		List<PlayerEntities> entites = playerInitService.getAllByPlayerId(playerId);
		
		if(entites.isEmpty()) {
			log.warn("No player entities found for playerId '{}'", playerId);
			return;
		}
		log.info("Found {} entities for player ID {}.", entites.size(), playerId);
		
		PlayerEntityListData.Builder listBuilder = PlayerEntityListData.newBuilder();
		
		for (PlayerEntities entity : entites) {
			PlayerEntityData entityData = PlayerEntityData.newBuilder()
					.setId(entity.getId())
					.setChunkX(entity.getChunkX())
					.setChunkY(entity.getChunkY())
					.setCreatedAt(ProtobufTimeUtil.toProtobufTimestamp(entity.getCreatedAt()))
					.setEntityType(entity.getEntityType())
					.setHealth(entity.getHealth())
					.setPosX(entity.getPosX())
					.setPosY(entity.getPosY())
					.setStateData(ProtobufJsonbUtil.toJsonString(entity.getStateData()))
					.setPlayerId(entity.getPlayerId())
					.setWorldId(entity.getWorldId())
					.setUpdatedAt(ProtobufTimeUtil.toProtobufTimestamp(entity.getUpdatedAt()))
					.setSectorId(entity.getSectorId())
					.setIsStatic(entity.getIsStatic())
					.build();
			
			listBuilder.addEntities(entityData);
		}
		
		PlayerEntityListData playerEntityListData = listBuilder.build();
		messageSender.sendBinaryProtobufMessage(ctx, playerEntityListData, NetworkBinaryCommands.REQUEST_PLAYER_ENTITIES_S.getCode());		
	}

}

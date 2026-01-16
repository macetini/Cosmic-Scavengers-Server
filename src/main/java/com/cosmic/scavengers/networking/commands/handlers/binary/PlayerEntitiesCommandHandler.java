package com.cosmic.scavengers.networking.commands.handlers.binary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandBinaryHandler;
import com.cosmic.scavengers.db.model.tables.pojos.PlayerEntities;
import com.cosmic.scavengers.db.services.jooq.PlayerInitService;
import com.cosmic.scavengers.networking.commands.NetworkBinaryCommand;
import com.cosmic.scavengers.networking.commands.dispatcher.MessageDispatcher;
import com.cosmic.scavengers.utils.protobuf.ProtobufTimeUtil;
import com.cosmicscavengers.networking.protobuf.entities.EntitySyncResponse;
import com.cosmicscavengers.networking.protobuf.entities.PlayerEntityProto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

@Component
public class PlayerEntitiesCommandHandler implements ICommandBinaryHandler {
	private static final Logger log = LoggerFactory.getLogger(PlayerEntitiesCommandHandler.class);

	private final MessageDispatcher messageDispatcher;
	private final PlayerInitService playerInitService;

	public PlayerEntitiesCommandHandler(MessageDispatcher messageDispatcher, PlayerInitService playerInitService) {
		this.messageDispatcher = messageDispatcher;
		this.playerInitService = playerInitService;
	}

	@Override
	public NetworkBinaryCommand getCommand() {
		return NetworkBinaryCommand.REQUEST_PLAYER_ENTITIES_C;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, ByteBuf payload) {
		log.info("Handling Log: [{}] command for channel: '{}'.", getCommand().getLogText(), ctx.channel().id());

		Long playerId = payload.readLong();
		this.handle(ctx, playerId);
	}
	
	void handle(ChannelHandlerContext ctx, Long playerId) {
		log.info("Fetching player entities for playerId '{}'", playerId);
		List<PlayerEntities> entities = playerInitService.fetchAndInitializeEntities(playerId);

		if (entities.isEmpty()) {
			log.warn("No player entities found for playerId '{}'", playerId);
			return;
		}
		log.info("Found {} entities for player ID {}.", entities.size(), playerId);

		EntitySyncResponse.Builder responseBuilder = EntitySyncResponse.newBuilder();
		
		responseBuilder.setWorldId(entities.get(0).getWorldId());
		responseBuilder.setSectorId(entities.get(0).getSectorId());

		for (PlayerEntities entity : entities) {            
            PlayerEntityProto proto = PlayerEntityProto.newBuilder()
                    .setId(entity.getId())
                    .setPlayerId(entity.getPlayerId())
                    .setWorldId(entity.getWorldId())
                    .setSectorId(entity.getSectorId())
                    .setBlueprintId(entity.getBlueprintId())
                    .setStatusId(entity.getStatusId())
                    .setEntityName(entity.getEntityName() != null ? entity.getEntityName() : "")
                    .setIsStatic(entity.getIsStatic())
                    .setPosX(entity.getPosX())
                    .setPosY(entity.getPosY())
                    .setPosZ(entity.getPosZ())
                    .setRotation(entity.getRotation())
                    .setChunkX(entity.getChunkX())
                    .setChunkY(entity.getChunkY())
                    .setCurrentHealth(entity.getCurrentHealth())                    
                    .setStateData(entity.getStateData() != null ? entity.getStateData().data() : "{}")                    
                    .setCreatedAt(ProtobufTimeUtil.toProtobufTimestamp(entity.getCreatedAt()))
                    .setUpdatedAt(ProtobufTimeUtil.toProtobufTimestamp(entity.getUpdatedAt()))
                    .build();
            responseBuilder.addEntities(proto);
        }
		
		EntitySyncResponse finalMessage = responseBuilder.build();
		log.info("Prepared EntitySyncResponse with {} entities for player ID {}.", finalMessage.getEntitiesCount(), playerId);	
		
		messageDispatcher.sendBinaryProtobufMessage(ctx, finalMessage,
				NetworkBinaryCommand.REQUEST_PLAYER_ENTITIES_S.getCode());
	}

}

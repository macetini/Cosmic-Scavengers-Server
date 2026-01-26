package com.cosmic.scavengers.networking.handlers.binary;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandBinaryHandler;
import com.cosmic.scavengers.core.utils.DecimalUtils;
import com.cosmic.scavengers.core.utils.ProtobufTimeUtil;
import com.cosmic.scavengers.db.jpa.model.BlueprintTemplate;
import com.cosmic.scavengers.db.model.tables.pojos.PlayerEntities;
import com.cosmic.scavengers.db.services.PlayerInitService;
import com.cosmic.scavengers.networking.MessageDispatcher;
import com.cosmic.scavengers.networking.commands.NetworkBinaryCommand;
import com.cosmic.scavengers.networking.mappers.TraitProtobufMapper;
import com.cosmic.scavengers.registries.BlueprintRegistry;
import com.cosmic.scavengers.registries.TraitRegistry;
import com.cosmicscavengers.networking.protobuf.entities.EntitySyncResponse;
import com.cosmicscavengers.networking.protobuf.entities.PlayerEntityProto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

@Component
public class PlayerEntitiesCommandHandler implements ICommandBinaryHandler {
	private static final Logger log = LoggerFactory.getLogger(PlayerEntitiesCommandHandler.class);

	private final MessageDispatcher messageDispatcher;
	private final PlayerInitService playerInitService;
	private final BlueprintRegistry blueprintRegistry;
	private final TraitRegistry traitRegistry;
	private final TraitProtobufMapper traitProtobufMapper;

	public PlayerEntitiesCommandHandler(
			MessageDispatcher messageDispatcher, 
			PlayerInitService playerInitService,
			BlueprintRegistry blueprintRegistry, 
			TraitRegistry traitRegisty,
			TraitProtobufMapper traitProtobufMapper) {
		this.messageDispatcher = messageDispatcher;
		this.playerInitService = playerInitService;
		this.blueprintRegistry = blueprintRegistry;
		this.traitRegistry = traitRegisty;
		this.traitProtobufMapper = traitProtobufMapper;
	}

	@Override
	public NetworkBinaryCommand getCommand() {
		return NetworkBinaryCommand.REQUEST_PLAYER_ENTITIES_C;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, ByteBuf payload) {
		log.info("Handling Command: [{}] | Channel: '{}'.", 
				getCommand().getLogText(), ctx.channel().id());

		Long playerId = payload.readLong();
		this.handle(ctx, playerId);
	}

	void handle(ChannelHandlerContext ctx, Long playerId) {
		log.info("Fetching player entities for PlayerId: '{}'", playerId);
		List<PlayerEntities> entities = 
				playerInitService.fetchAndInitializeEntities(playerId);

		if (entities.isEmpty()) {
			log.error("No player entities found for playerId '{}'", playerId);
			return;
		}
		log.debug("Found {} entities for player ID {}.", entities.size(), playerId);

		EntitySyncResponse.Builder responseBuilder = EntitySyncResponse.newBuilder();

		PlayerEntities firstEntity = entities.get(0);
		responseBuilder.setWorldId(firstEntity.getWorldId());
		responseBuilder.setSectorId(firstEntity.getSectorId());

		for (PlayerEntities entity : entities) {
			PlayerEntityProto proto = buildPlayerEntityProto(entity);
			responseBuilder.addEntities(proto);
		}

		EntitySyncResponse finalMessage = responseBuilder.build();
		log.debug("Prepared EntitySyncResponse with {} entities for player ID {}.", 
				finalMessage.getEntitiesCount(), playerId);

		messageDispatcher.sendBinaryProtobufMessage(ctx, finalMessage, 
				NetworkBinaryCommand.REQUEST_PLAYER_ENTITIES_S.getCode());
	}

	// TODO - Split into multipe methods
	private PlayerEntityProto buildPlayerEntityProto(PlayerEntities entity) {
		Long playerId = entity.getPlayerId();
		long entityId = entity.getId();
		String bluprintId = entity.getBlueprintId();
		
		log.debug("Building Proto for PlayerId: '{}' | EntityId: '{}' |  With Blueptrint: '{}'", 
				playerId, entityId, bluprintId);

		PlayerEntityProto.Builder entityBuilder = 
				PlayerEntityProto.newBuilder()
				.setId(entity.getId())
				.setPlayerId(playerId)
				.setWorldId(entity.getWorldId())
				.setSectorId(entity.getSectorId()).setBlueprintId(bluprintId)
				.setStatusId(entity.getStatusId())
				.setEntityName(entity.getEntityName() != null ? entity.getEntityName() : "")
				.setIsStatic(entity.getIsStatic())
				.setPosX(DecimalUtils.toScaled(entity.getPosX()))
				.setPosY(DecimalUtils.toScaled(entity.getPosY()))
				.setPosZ(DecimalUtils.toScaled(entity.getPosZ()))
				.setRotation(entity.getRotation())
				.setChunkX(entity.getChunkX())
				.setChunkY(entity.getChunkY())
				.setCurrentHealth(entity.getCurrentHealth())
				//.setStateData(entity.getStateData())
				.setCreatedAt(ProtobufTimeUtil.toProtobufTimestamp(entity.getCreatedAt()))
				.setUpdatedAt(ProtobufTimeUtil.toProtobufTimestamp(entity.getUpdatedAt()));

		BlueprintTemplate blueprint = blueprintRegistry.get(bluprintId)
				.orElseThrow(() -> new IllegalStateException("Failed to find blueprint with ID: " + bluprintId));		

		blueprint.traitIds().forEach(
				traitId -> traitRegistry.get(traitId).ifPresentOrElse(trait -> 
				{
					log.trace("Processing Trait: [{}] for PlayerId: '{}' EntityId '{}' - With Trait Properties: [{}]",
							traitId, playerId, entityId, trait);
					
					traitProtobufMapper.mapToProto(traitId, trait).ifPresent(entityBuilder::addTraits);
				},
				() -> log.warn("While Parsing PlayerId: '{}' traits JSON for EntityId '{}': Failed to find Trait with ID: {}",
						playerId, entityId, traitId)));	

		return entityBuilder.build();
	}	
}

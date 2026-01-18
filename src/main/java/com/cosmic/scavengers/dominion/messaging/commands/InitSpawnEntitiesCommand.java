package com.cosmic.scavengers.dominion.messaging.commands;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.db.model.tables.pojos.PlayerEntities;
import com.cosmic.scavengers.dominion.components.Owner;
import com.cosmic.scavengers.dominion.components.Position;
import com.cosmic.scavengers.dominion.messaging.meta.IEcsCommand;
import com.cosmic.scavengers.dominion.tags.StaticTag;
import com.cosmic.scavengers.gameplay.registry.EntityRegistry;
import com.cosmic.scavengers.utils.DecimalUtils;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public record InitSpawnEntitiesCommand(long playerId, List<PlayerEntities> entitiesData) implements IEcsCommand {

	private static final Logger log = LoggerFactory.getLogger(InitSpawnEntitiesCommand.class);

	@Override
	public void execute(Dominion dominion, EntityRegistry entityRegistry) {
		log.info("Starting ECS Entity registration.");
		for (PlayerEntities data : entitiesData) {
			final Long enitityId = data.getId();
			
			if (entityRegistry.isActive(enitityId)) {
				log.warn("Entity {} is already active. Skipping spawn command.", enitityId);
				continue;
			}
			
            final Position initialPos = new Position(
                DecimalUtils.fromFloat(data.getPosX()),
                DecimalUtils.fromFloat(data.getPosY()), 
                DecimalUtils.fromFloat(data.getPosZ())
            );
			final Owner owner = new Owner(data.getPlayerId());
			
			// PlayerID|EntityID|Name
			final String entityName = String.format("%d|%d|%s", 
				    data.getPlayerId(), 
				    enitityId, 
				    data.getEntityName()
				);
			final Entity liveEntity = dominion.createEntity(entityName, initialPos, owner);		
			
			if (Boolean.TRUE.equals(data.getIsStatic())) {
				liveEntity.add(new StaticTag());
			}

			entityRegistry.register(enitityId, liveEntity);
		}

		log.info("ECS Entity registration finished.");
	}
}
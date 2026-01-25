package com.cosmic.scavengers.ecs.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.ecs.commands.meta.IEcsCommand;
import com.cosmic.scavengers.ecs.domain.components.Owner;
import com.cosmic.scavengers.ecs.domain.intents.MoveIntent;
import com.cosmic.scavengers.ecs.domain.tags.StaticTag;
import com.cosmic.scavengers.gameplay.services.data.MoveRequestData;
import com.cosmic.scavengers.registries.EntityRegistry;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public record MoveEntityCommand(MoveRequestData data) implements IEcsCommand {
	private static final Logger log = LoggerFactory.getLogger(MoveEntityCommand.class);

	@Override
	public void execute(Dominion dominion, EntityRegistry entityRegistry) {
		long entityId = data.entityId();
		Long playerId = data.playerId();

		log.info("Handling ECS Move Command for Player Id '{}' Entity Id '{}'. Target: [{}, {}, {}]",
				playerId, entityId, 
				data.targetX(), data.targetY(), data.targetZ());

		Entity liveEntity = entityRegistry.getLiveEntity(entityId);
		if (liveEntity == null) {
			log.warn("Move rejected: Entity Id '{}' not found in registry.", entityId);
			return;
		}

		Owner owner = liveEntity.get(Owner.class);
		if (owner == null || owner.playerId() != playerId) {
			log.error("Cheat Attempt: (Wrong)Player Id '{}' tried to move entity '{}' owned by Player Id'{}'",
					playerId, entityId,
					owner != null ? playerId : "none");
			return;
		}

		if (liveEntity.has(StaticTag.class)) {
			log.warn("Move rejected: Entity {} is static.", entityId);
			return;
		}

		MoveIntent intent = new MoveIntent(entityId, playerId, data);

		if (liveEntity.has(MoveIntent.class)) {
			MoveIntent existing = liveEntity.get(MoveIntent.class);
			if (existing != null) {
				liveEntity.remove(existing);
			}
		}
		liveEntity.add(intent);
	}
}

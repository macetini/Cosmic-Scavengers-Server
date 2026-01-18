package com.cosmic.scavengers.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.dominion.components.Movement;
import com.cosmic.scavengers.dominion.intents.MoveIntent;
import com.cosmic.scavengers.gameplay.services.entities.data.MoveRequestData;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * The "Gatekeeper" System. Converts transient player MoveIntents into
 * persistent Movement states.
 */
@Component
public class IntentProcessorSystem implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(IntentProcessorSystem.class);
	
	private final Dominion dominion;

	public IntentProcessorSystem(Dominion dominion) {
		this.dominion = dominion;
	}

	@Override
	public void run() {
		dominion.findEntitiesWith(MoveIntent.class).stream().forEach(result -> {
			Entity entity = result.entity();
			MoveIntent intent = result.comp();
			
			MoveRequestData requsetData = intent.requsetData();

			log.debug("Processing Move Intent for Player '{}' Entity: '{}': Moving to [{},{},{}]", 
					requsetData.playerId(),
					requsetData.entityId(),
					requsetData.targetX(), requsetData.targetY(), requsetData.targetZ());

			Movement movementState = new Movement(
					requsetData.targetX(), requsetData.targetY(), requsetData.targetZ(), 
					requsetData.movementSpeed());
			
			entity.remove(intent);
			entity.add(movementState);			
		});
	}
}
package com.cosmic.scavengers.gameplay.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosmic.scavengers.db.model.tables.pojos.PlayerEntities;
import com.cosmic.scavengers.ecs.commands.MoveEntityCommand;
import com.cosmic.scavengers.ecs.queue.EcsCommandQueue;
import com.cosmic.scavengers.gameplay.services.data.MoveRequestData;

@Service
public class EntityActionService {
	private static final Logger log = LoggerFactory.getLogger(EntityActionService.class);
	
	private final EcsCommandQueue dominionCommandQueue;	

	public EntityActionService(EcsCommandQueue dominionCommandQueue) {
		this.dominionCommandQueue = dominionCommandQueue;
	}

	/**
	 * Authoritatively processes a move intent.
	 */
	@Transactional
	public void processMoveRequest(MoveRequestData data) {
		log.info("Dispatching ECS Move Command for Player Id '{}' move request for Entity Id '{}'.",
				data.playerId(), data.entityId());
		
		final MoveEntityCommand command = new MoveEntityCommand(data);
		dominionCommandQueue.submit(command);

//		PlayerEntities entity = dsl.selectFrom(PLAYER_ENTITIES).where(PLAYER_ENTITIES.ID.eq(data.entityId()))
//				.and(PLAYER_ENTITIES.PLAYER_ID.eq(playerId)).fetchOneInto(PlayerEntities.class);
//
//		if (entity == null) {
//			log.error("Auth Failed: Player {} does not own Entity {}", playerId, data.entityId());
//			return;
//		}
//		if (Boolean.TRUE.equals(entity.getIsStatic())) {
//			log.error("Move Rejected: Entity {} is marked as static (Building).", data.entityId());
//			return;
//		}
//		if (!isValidMoveRequest(entity, data)) {
//			log.error(
//					"Anti-Cheat Check Fail: Move request for Player Id '{}' Entity Id '{}' not accepted, possible cheat.",
//					playerId, data.entityId());
//			return;
//		}
//
//		try {
//			ObjectNode root = (ObjectNode) jsonMapper.readTree(entity.getStateData().data());
//
//			ObjectNode dataNode = root.withObject("/traits/MOVABLE/data");
//
//			dataNode.put("target_x", data.x().unscaledValue());
//			dataNode.put("target_y", data.y().unscaledValue());
//			dataNode.put("target_z", data.z().unscaledValue());
//
//			dataNode.put("movement_speed", data.movementSpeed().unscaledValue());
//			dataNode.put("rotation_speed", data.rotationSpeed().unscaledValue());
//			dataNode.put("stopping_distance", data.stoppingDistance().unscaledValue());
//
//			dataNode.put("status", "MOVING");
//
//			// Update Metadata on the trait level
//			root.withObject("/traits/MOVABLE").put("intent_timestamp", System.currentTimeMillis());
//
//			dsl.update(PLAYER_ENTITIES)
//					.set(PLAYER_ENTITIES.STATE_DATA, JSONB.valueOf(jsonMapper.writeValueAsString(root)))
//					.set(PLAYER_ENTITIES.UPDATED_AT, OffsetDateTime.now()).where(PLAYER_ENTITIES.ID.eq(data.entityId()))
//					.execute();
//
//			log.info("Entity {} moving to target", data.entityId());
//
//		} catch (Exception e) {
//			log.error("JSON Processing Error for entity {}: {}", data.entityId(), e.getMessage());
//		}

	}

	/**
	 * Validates the move request against the current state of the entity. Checks
	 * for speed hacking and teleportation.
	 */
	private boolean isValidMoveRequest(PlayerEntities entity, MoveRequestData data) {
//		double requestedSpeed = data.movementSpeed().doubleValue();
//
//		// Basic Speed Check
//		// In a production environment, compare against entity.getBaseSpeed()
//		if (requestedSpeed > MAX_ALLOWED_SPEED || requestedSpeed <= 0) {
//			log.warn("[Anti-Cheat] Player {} sent invalid speed: {} for Entity {}", entity.getPlayerId(),
//					requestedSpeed, entity.getId());
//			return false;
//		}
//
//		// Teleportation Check (Distance from current position)
//		double currentX = entity.getPosX().doubleValue();
//		double currentY = entity.getPosY().doubleValue();
//		double currentZ = entity.getPosZ().doubleValue();
//
//		double targetX = data.x().doubleValue();
//		double targetY = data.y().doubleValue();
//		double targetZ = data.z().doubleValue();
//
//		// Pythagorean theorem (3D distance between Target and Current position)
//		double distance = Math.sqrt(
//				Math.pow(targetX - currentX, 2) + Math.pow(targetY - currentY, 2) + Math.pow(targetZ - currentZ, 2));
//
//		if (distance > MAX_TELEPORT_DISTANCE) {
//			log.warn("[Anti-Cheat] Player {} attempted teleport: Distance {} for Entity {}", entity.getPlayerId(),
//					distance, entity.getId());
//			return false;
//		}

		return true;
	}
}

package com.cosmic.scavengers.registries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dev.dominion.ecs.api.Entity;

/**
 * A high-performance registry that bridges Database IDs to live Dominion
 * Entities. This allows services and network handlers to find live game objects
 * instantly.
 */
@Component
public class EntityRegistry {
	private static final Logger log = LoggerFactory.getLogger(EntityRegistry.class);

	// ConcurrentHashMap provides thread-safety for high-frequency access
	private final Map<Long, Entity> liveEntities = new ConcurrentHashMap<>();

	/**
	 * Registers a live entity into the world.
	 * 
	 * @param dbId           The Primary Key from the PLAYER_ENTITIES table.
	 * @param dominionEntity The live ECS entity instance.
	 */
	public void register(long dbId, Entity dominionEntity) {
		liveEntities.put(dbId, dominionEntity);
		log.debug("Registered live entity: ID {}", dbId);
	}

	/**
	 * Retrieves a live entity by its database ID.
	 */
	public Entity getLiveEntity(long dbId) {
		return liveEntities.get(dbId);
	}

	/**
	 * Removes an entity from the registry (e.g., when a player logs out or entity
	 * is destroyed).
	 * 
	 * 	 * @param entityDbId The Primary Key from the PLAYER_ENTITIES table.
	 */
	public void unregister(long dbId) {
		Entity removed = liveEntities.remove(dbId);
		if (removed != null) {
			log.debug("Unregistered entity: ID {}", dbId);
		}
	}

	/**
	 * Checks if an entity is currently active in the simulation.
	 * 
	 * @param entityDbId The Primary Key from the PLAYER_ENTITIES table.
	 */
	public boolean isActive(long entityDbId) {
		return liveEntities.containsKey(entityDbId);
	}

	public int getActiveCount() {
		return liveEntities.size();
	}
}
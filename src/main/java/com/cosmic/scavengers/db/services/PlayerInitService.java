package com.cosmic.scavengers.db.services;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cosmic.scavengers.db.jooq.repositories.PlayerEntitiyRepository;
import com.cosmic.scavengers.db.jooq.repositories.WorldRepository;
import com.cosmic.scavengers.db.model.tables.pojos.PlayerEntities;
import com.cosmic.scavengers.db.model.tables.pojos.Worlds;
import com.cosmic.scavengers.ecs.commands.InitSpawnEntitiesCommand;
import com.cosmic.scavengers.ecs.queue.EcsCommandQueue;

@Service
public class PlayerInitService {
	private static final Logger log = LoggerFactory.getLogger(PlayerInitService.class);

	private final WorldRepository jooqWorldRepository;
	private final PlayerEntitiyRepository jooqPlayerEntityRepository;	
	private final EcsCommandQueue dominionCommandQueue;

	public PlayerInitService(WorldRepository jooqWorldRepository,
			PlayerEntitiyRepository jooqPlayerEntityRepository,			
			EcsCommandQueue dominionCommandQueue) {
		this.jooqWorldRepository = jooqWorldRepository;
		this.jooqPlayerEntityRepository = jooqPlayerEntityRepository;		
		this.dominionCommandQueue = dominionCommandQueue;

	}

	public Worlds getCurrentWorldDataByPlayerId(long playerId) {
		log.info("Fetching world data for player {}", playerId);

		final Optional<Worlds> worldOptional = jooqWorldRepository.getById(playerId);
		return worldOptional
				.orElseThrow(() -> new IllegalStateException("No world data found for player with ID: " + playerId));
	}

	/**
	 * Fetches entities from DB and ensures they exist in the live ECS simulation.
	 */
	public List<PlayerEntities> fetchAndInitializeEntities(long playerId) {
		log.info("Fetching entities for player {}", playerId);
		final List<PlayerEntities> entities = jooqPlayerEntityRepository.getAllByPlayerId(playerId);

		InitSpawnEntitiesCommand spawnCommand = new InitSpawnEntitiesCommand(playerId, entities);
		dominionCommandQueue.submit(spawnCommand);

		return entities;
	}
}

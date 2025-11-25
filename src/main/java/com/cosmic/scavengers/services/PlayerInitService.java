package com.cosmic.scavengers.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosmic.scavengers.db.PlayerEntityRepository;
import com.cosmic.scavengers.db.meta.Player;
import com.cosmic.scavengers.db.meta.PlayerEntity;

/**
 * Service dedicated to managing the initial game state creation for a player.
 * This includes idempotent checks to ensure a player always has required
 * starting entities.
 */
@Service
public class PlayerInitService {
	private static final Logger log = LoggerFactory.getLogger(PlayerInitService.class);

	private final PlayerEntityRepository playerEntityRepository;

	public PlayerInitService(PlayerEntityRepository playerEntityRepository) {
		this.playerEntityRepository = playerEntityRepository;
	}

	/**
	 * Public method called during login/registration to ensure the player's core
	 * game entities exist. This is the new, centralized entry point for initial
	 * game state setup.
	 * 
	 * @param player The authenticated Player object.
	 * @return void
	 * 
	 */
	@Transactional // Ensures the entity creation is an atomic database operation
	public void ensurePlayerInitialized(Player player) {
		// We only check for the MAIN_BASE for simplicity, but more complex logic
		// could check for all required starting resources/units.
		createMainBaseIfMissing(player);

		// Add other initialization calls here (e.g., giveStartingUnitsIfMissing)
	}

	/**
	 * Checks if the player has any entities and creates a "MAIN_BASE" at (0, 0) if
	 * none exist. This is an idempotent check.
	 */
	private void createMainBaseIfMissing(Player player) {
		List<PlayerEntity> entities = playerEntityRepository.findAllByPlayerId(player.getId());

		if (entities.isEmpty()) {

			log.info("Creating initial MAIN_BASE for player (ID: {} - UName: {}).", player.getId(),
					player.getUsername());

			// Default starting values for a new base
			// FIX: Changed coordinate literals from 0.0 (double) to 0 (int)
			// to match the PlayerEntity constructor signature (int posX, int posY).
			PlayerEntity mainBase = new PlayerEntity(player.getId(), "MAIN_BASE", 0, // Corrected to int
					0, // Corrected to int
					500, "{\"resource\": 50}");

			playerEntityRepository.save(mainBase);
		}
	}
}
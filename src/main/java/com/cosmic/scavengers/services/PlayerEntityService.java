package com.cosmic.scavengers.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosmic.scavengers.db.meta.PlayerEntity;
import com.cosmic.scavengers.db.repos.PlayerEntityRepository;
import com.cosmic.scavengers.networking.meta.PlayerEntityDTO;

/**
 * Service layer for managing PlayerEntity data. This class orchestrates data
 * access (via the repository) and applies business logic.
 */
@Service
public class PlayerEntityService {

	private final PlayerEntityRepository playerEntityRepository;

	/**
	 * Constructor injection for the PlayerEntityRepository. Spring automatically
	 * provides the required instance.
	 */
	public PlayerEntityService(PlayerEntityRepository playerEntityRepository) {
		this.playerEntityRepository = playerEntityRepository;
	}

	/**
	 * Retrieves all PlayerEntity objects owned by a specific player ID. * @param
	 * playerId The ID of the Player.
	 * 
	 * @return A list of PlayerEntity objects.
	 */
	@Transactional(readOnly = true)
	public List<PlayerEntityDTO> getEntitiesByPlayerId(Long playerId) {		
		List<PlayerEntity> playerEntites = playerEntityRepository.findAllByPlayerId(playerId);
		return playerEntites.stream().map(PlayerEntityDTO::fromEntity).toList();
	}

	/**
	 * Retrieves a PlayerEntity by its primary key ID. * @param id The primary key
	 * ID of the entity.
	 * 
	 * @return An Optional containing the PlayerEntity if found, otherwise empty.
	 */
	@Transactional(readOnly = true)
	public Optional<PlayerEntity> getEntityById(Long id) {
		return playerEntityRepository.findById(id);
	}

	/**
	 * Saves or updates a PlayerEntity object. * @param entity The PlayerEntity to
	 * save.
	 * 
	 * @return The persisted PlayerEntity object.
	 */
	@Transactional
	public PlayerEntity saveOrUpdateEntity(PlayerEntity entity) {
		// Business logic (e.g., validation) would go here before saving
		return playerEntityRepository.save(entity);
	}

	/**
	 * Deletes a PlayerEntity by its primary key ID. * @param id The primary key ID
	 * of the entity to delete.
	 */
	@Transactional
	public void deleteEntity(Long id) {
		playerEntityRepository.deleteById(id);
	}
}
package com.cosmic.scavengers.db.jooq.repositories;

import static com.cosmic.scavengers.db.model.tables.PlayerEntities.PLAYER_ENTITIES;

import java.util.List;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.cosmic.scavengers.db.model.tables.pojos.PlayerEntities;

@Repository
public class PlayerEntitiyRepository {

	private final DSLContext dsl;

	public PlayerEntitiyRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	/**
	 * Finds all player entities owned by a specific player ID.
	 *
	 * @param playerId The ID of the player whose entities are to be retrieved.
	 * @return A list of Players entities owned by the specified player ID.
	 */
	public List<PlayerEntities> getAllByPlayerId(Long playerId) {
		return dsl
				.selectFrom(PLAYER_ENTITIES)
				.where(PLAYER_ENTITIES.PLAYER_ID.eq(playerId))
				.fetchInto(PlayerEntities.class);
	}

}

package com.cosmic.scavengers.db.jooq.repositories;

import static com.cosmic.scavengers.db.model.tables.Worlds.WORLDS;

import java.util.Optional;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.cosmic.scavengers.db.model.tables.pojos.Worlds;

@Repository
@Transactional(readOnly = true)
public class WorldRepository {
	private final DSLContext dsl;

	public WorldRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	public Optional<Worlds> getById(long id) {
		return dsl.selectFrom(WORLDS).where(WORLDS.ID.eq(id)).fetchOptional().map(r -> r.into(Worlds.class));
	}

	public Optional<Worlds> getDefaultWorld() {
		return getById(1L);
	}

}

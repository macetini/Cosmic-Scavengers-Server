package com.cosmic.scavengers.db.jpa.repositories;

import com.cosmic.scavengers.db.jpa.domain.EntityBlueprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Standard JPA Repository for the entity_blueprints table. Used by
 * BlueprintService to load "Cold" data from Postgres.
 */
@Repository
public interface EntityBlueprintRepository extends JpaRepository<EntityBlueprint, String> {

	/**
	 * Useful for filtering blueprints by their category (e.g., 'UNIT', 'STRUCTURE')
	 * during specialized ingestion or registry filtering.
	 */
	List<EntityBlueprint> findByCategoryId(String categoryId);
}
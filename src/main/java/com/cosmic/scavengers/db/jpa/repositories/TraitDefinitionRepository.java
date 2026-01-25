package com.cosmic.scavengers.db.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cosmic.scavengers.db.jpa.domain.TraitDefinition;

@Repository
public interface TraitDefinitionRepository extends JpaRepository<TraitDefinition, String> {
	// Standard JpaRepository methods provide all the necessary CRUD
}
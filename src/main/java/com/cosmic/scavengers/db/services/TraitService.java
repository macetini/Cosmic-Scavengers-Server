package com.cosmic.scavengers.db.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosmic.scavengers.db.jpa.domain.TraitDefinition;
import com.cosmic.scavengers.db.jpa.repositories.TraitDefinitionRepository;

@Service
public class TraitService {

	private final TraitDefinitionRepository traitRepository;

	public TraitService(TraitDefinitionRepository traitRepository) {
		this.traitRepository = traitRepository;
	}

	/**
	 * Used by the TraitRegistry at startup to populate the cache.
	 */
	@Transactional(readOnly = true)
	public List<TraitDefinition> findAllDefinitions() {
		return traitRepository.findAll();
	}

	/**
	 * Used by the Ingestion service to save traits from YAML. Here you can add
	 * validation logic later.
	 */
	@Transactional
	public TraitDefinition saveDefinition(TraitDefinition definition) {
		// Example validation: Ensure ID is lowercase for consistency
		if (definition.getId() != null) {
			definition.setId(definition.getId().toLowerCase());
		}
		return traitRepository.save(definition);
	}

	/**
	 * Single lookup if needed (usually for admin/tooling tasks).
	 */
	@Transactional(readOnly = true)
	public Optional<TraitDefinition> findById(String id) {
		return traitRepository.findById(id);
	}
}
package com.cosmic.scavengers.db.ingestion;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosmic.scavengers.core.db.AbstractYamlIngester;
import com.cosmic.scavengers.db.jpa.domain.TraitDefinition;
import com.cosmic.scavengers.db.jpa.repositories.IngestionMetadataRepository;
import com.cosmic.scavengers.db.jpa.repositories.TraitDefinitionRepository;

@Service
public class TraitsIngestionService extends AbstractYamlIngester {
	private static final Logger log = LoggerFactory.getLogger(TraitsIngestionService.class);
	private static final String DIRECTORY = "traits";

	private final TraitDefinitionRepository traitRepo;

	public TraitsIngestionService(IngestionMetadataRepository metaRepo, TraitDefinitionRepository traitRepo) {
		super(metaRepo);
		this.traitRepo = traitRepo;
	}

	/**
	 * Entry point called by the DataInitializer. Scans 'classpath:traits/*.yaml'
	 * and processes changes.
	 */	
	@Transactional
	public void sync() {
		this.syncDirectory(DIRECTORY, this::processTraitData);
	}

	/**
	 * The implementation of the BiConsumer expected by syncDirectory. Maps the raw
	 * YAML data to our JPA Entity.
	 */
	private void processTraitData(Map<String, Map<String, Object>> data, String category) {		
		log.debug("Synchronizing {} Trait definitions for category: [{}]", data.size(), category);

		data.forEach((traitId, properties) -> {			
			TraitDefinition trait = traitRepo.findById(traitId).orElse(new TraitDefinition());

			trait.setId(traitId);
			trait.setCategory(category.toUpperCase());
			trait.setData(properties);

			traitRepo.save(trait);
			log.trace("Synced trait: {}", traitId);
		});
	}
}

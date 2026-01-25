package com.cosmic.scavengers.db.ingestion;

import java.time.OffsetDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosmic.scavengers.core.db.AbstractYamlIngester;
import com.cosmic.scavengers.db.jpa.domain.EntityBlueprint;
import com.cosmic.scavengers.db.jpa.repositories.EntityBlueprintRepository;
import com.cosmic.scavengers.db.jpa.repositories.IngestionMetadataRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class BlueprintIngestionService extends AbstractYamlIngester {
	private static final Logger log = LoggerFactory.getLogger(BlueprintIngestionService.class);

	private static final String DIRECTORY = "entity_blueprints";
	private final EntityBlueprintRepository blueprintRepo;
	private final ObjectMapper mapper;

	public BlueprintIngestionService(
			IngestionMetadataRepository metaRepo, 
			EntityBlueprintRepository blueprintRepo,
			ObjectMapper mapper) {
		super(metaRepo);
		
		this.blueprintRepo = blueprintRepo;
		this.mapper = mapper;
	}

	/**
	 * Entry point called by the DataInitializer. Scans
	 * 'classpath:definitions/entity_blueprints/*.yaml' and processes changes.
	 */
	@Transactional
	public void sync() {
		this.syncDirectory(DIRECTORY, this::processBlueprintData);
	}

	private void processBlueprintData(Map<String, Map<String, Object>> data, String category) {
		log.debug("Synchronizing {} Blueprint definitions for category: [{}]", data.size(), category);

		data.forEach((rawId, properties) -> {
			String sanitizedId = rawId.replace(" ", "_").toUpperCase();
			if (!rawId.toUpperCase().equals(sanitizedId)) {
				log.warn("Blueprint ID '{}' sanitized to '{}'.", rawId, sanitizedId);
			}

			EntityBlueprint blueprint = blueprintRepo.findById(sanitizedId).orElseGet(() -> {
				EntityBlueprint newBp = new EntityBlueprint();
				newBp.setId(sanitizedId);
				newBp.setCreatedAt(OffsetDateTime.now());
				newBp.setVersion(0);
				return newBp;
			});

			try {
				mapper.updateValue(blueprint, properties);
			} catch (Exception e) {
				log.error("Failed to map blueprint [{}]: {}", sanitizedId, e.getMessage());
				throw new RuntimeException(e);
			}

			blueprint.setCategoryId(category.toUpperCase());
			blueprint.setUpdatedAt(OffsetDateTime.now());

			blueprintRepo.saveAndFlush(blueprint);
			log.trace("Synced blueprint: {}", sanitizedId);
		});
	}
}
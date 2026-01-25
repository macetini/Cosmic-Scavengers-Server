package com.cosmic.scavengers.registries;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.db.jpa.domain.TraitDefinition;
import com.cosmic.scavengers.db.jpa.model.BlueprintTemplate;
import com.cosmic.scavengers.db.services.BlueprintService;

@Component
public class BlueprintRegistry {
	private static final Logger log = LoggerFactory.getLogger(BlueprintRegistry.class);

	private final BlueprintService blueprintService;
	private final Map<String, BlueprintTemplate> cache = new ConcurrentHashMap<>();

	public BlueprintRegistry(BlueprintService blueprintService) {
		this.blueprintService = blueprintService;
	}

	public void load() {
		log.debug("Caching Entity Blueprints from DB.");

		cache.clear();

		for (BlueprintTemplate def : blueprintService.loadAllTemplates()) {
			log.trace("Chaching BlueprintId '{}'", def.id());
			cache.put(def.id(), def);
		}
		
		log.debug("Successfully cached {} Blueprints.", cache.size());
	}

	public Optional<BlueprintTemplate> get(String id) {
		return Optional.ofNullable(cache.get(id));
	}

	public Collection<BlueprintTemplate> getAll() {
		return cache.values();
	}
	
	public int getCount() {
		return cache.size();
	}
}
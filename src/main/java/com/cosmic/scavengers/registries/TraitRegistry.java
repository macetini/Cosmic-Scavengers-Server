package com.cosmic.scavengers.registries;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.db.jpa.domain.TraitDefinition;
import com.cosmic.scavengers.db.services.TraitService;

/**
 * High-performance, in-memory cache for Trait Definitions. This is the primary
 * source of truth for the game loop.
 */
@Component
public class TraitRegistry {
	private static final Logger log = LoggerFactory.getLogger(TraitRegistry.class);

	private final TraitService traitService;
	private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();

	public TraitRegistry(TraitService traitService) {
		this.traitService = traitService;
	}

	/**
	 * Loads or reloads all traits from the database via the TraitService.
	 */
	public void load() {
		log.debug("Caching Traits from DB.");

		cache.clear();

		for (TraitDefinition def : traitService.findAllDefinitions()) {
			log.trace("Chaching TraitId '{}'", def.getId());
			cache.put(def.getId(), def.getData());			
		}
		log.debug("Successfully cached {} Traits.", cache.size());
	}

	/**
	 * Retrieves the configuration map for a specific trait. 
	 
	 * @param id The if of the trait (e.g., "movable")
	 * 
	 * @return The Map of properties, or null if not found
	 */
	public Optional<Map<String, Object>> get(String id) {
		return Optional.ofNullable(cache.get(id));
	}

	public Iterable<Map<String, Object>> getAll() {
		return cache.values();
	}

	public int getCount() {
		return cache.size();
	}
}
package com.cosmic.scavengers.db.services;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosmic.scavengers.db.jpa.domain.EntityBlueprint;
import com.cosmic.scavengers.db.jpa.model.BlueprintTemplate;
import com.cosmic.scavengers.db.jpa.repositories.EntityBlueprintRepository;

@Service
public class BlueprintService {

	private final EntityBlueprintRepository repository;

	public BlueprintService(EntityBlueprintRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<BlueprintTemplate> loadAllTemplates() {
		return repository.findAll().stream().map(this::mapToTemplate).toList(); // Java 17+ convenient list collector
	}

	private BlueprintTemplate mapToTemplate(EntityBlueprint entity) {
		Map<String, Object> configs = entity.getBehaviorConfigs();

		return new BlueprintTemplate(entity.getId(), entity.getCategoryId(), entity.getBaseHealth(),
				entity.isStaticDefault(), extractList(configs, "traits"), extractList(configs, "buffs"));
	}

	@SuppressWarnings("unchecked")
	private List<String> extractList(Map<String, Object> map, String key) {
		if (map == null)
			return List.of();
		Object value = map.get(key);
		return (value instanceof List<?> list) ? (List<String>) list : List.of();
	}
}
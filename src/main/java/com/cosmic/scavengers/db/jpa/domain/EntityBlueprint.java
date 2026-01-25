package com.cosmic.scavengers.db.jpa.domain;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.cosmic.scavengers.core.db.JsonToMapConverter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "entity_blueprints")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EntityBlueprint {

	@Id
	@Column(length = 50)
	private String id;

	@Column(name = "category_id", nullable = false, length = 20)
	private String categoryId;

	@Column(name = "base_health", nullable = false)
	private int baseHealth;

	@Column(name = "is_static_default", nullable = false)
	private boolean isStaticDefault;

	@Convert(converter = JsonToMapConverter.class)
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "behavior_configs", columnDefinition = "jsonb", nullable = false)
	@JsonProperty("behaviorConfigs")
	private Map<String, Object> behaviorConfigs = new HashMap<>();

	@jakarta.persistence.Version
	private Integer version = 0;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	// Required Default Constructor
	public EntityBlueprint() {
		// Hibernate Domain class, not to be instantiated
	}

	// Standard Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public int getBaseHealth() {
		return baseHealth;
	}

	public void setBaseHealth(int baseHealth) {
		this.baseHealth = baseHealth;
	}

	public boolean isStaticDefault() {
		return isStaticDefault;
	}

	public void setStaticDefault(boolean isStaticDefault) {
		this.isStaticDefault = isStaticDefault;
	}

	public Map<String, Object> getBehaviorConfigs() {
		return behaviorConfigs;
	}

	public void setBehaviorConfigs(Map<String, Object> behaviorConfigs) {
		this.behaviorConfigs = behaviorConfigs;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	// Standard Equals and HashCode (based on ID)
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		EntityBlueprint that = (EntityBlueprint) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "EntityBlueprint{" + "id='" + id + '\'' + ", categoryId='" + categoryId + '\'' + ", baseHealth="
				+ baseHealth + '}';
	}
}
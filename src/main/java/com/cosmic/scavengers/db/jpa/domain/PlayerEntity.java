package com.cosmic.scavengers.db.jpa.domain;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.cosmic.scavengers.core.db.JsonToMapConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "player_entities")
public class PlayerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "player_id", nullable = false)
	private Long playerId;

	@Column(name = "world_id", nullable = false)
	private Long worldId;

	@Column(name = "sector_id", nullable = false)
	private Long sectorId;

	@Column(name = "blueprint_id", nullable = false, length = 50)
	private String blueprintId;

	@Column(name = "status_id", nullable = false, length = 20)
	private String statusId = "ACTIVE";

	@Column(name = "entity_name", length = 100)
	private String entityName;

	@Column(name = "is_static", nullable = false)
	private boolean isStatic;

	@Column(name = "pos_x", nullable = false)
	private float posX;

	@Column(name = "pos_y", nullable = false)
	private float posY;

	@Column(name = "pos_z", nullable = false)
	private float posZ = 0.0f;

	@Column(name = "rotation", nullable = false)
	private float rotation = 0.0f;

	// GENERATED ALWAYS in Postgres: we only read these, never write
	@Column(name = "chunk_x", insertable = false, updatable = false)
	private Integer chunkX;

	@Column(name = "chunk_y", insertable = false, updatable = false)
	private Integer chunkY;

	@Column(name = "current_health", nullable = false)
	private int currentHealth;

	@Convert(converter = JsonToMapConverter.class)
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "state_data", columnDefinition = "jsonb", nullable = false)
	private Map<String, Object> stateData = new HashMap<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	// --- Constructor ---

	public PlayerEntity() {
		// Hibernate Domain class, not to be instantiated
	}

	// --- Lifecycle Hooks ---

	@PrePersist
	protected void onCreate() {
		this.createdAt = OffsetDateTime.now();
		this.updatedAt = OffsetDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	// --- Getters and Setters ---

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}

	public Long getWorldId() {
		return worldId;
	}

	public void setWorldId(Long worldId) {
		this.worldId = worldId;
	}

	public Long getSectorId() {
		return sectorId;
	}

	public void setSectorId(Long sectorId) {
		this.sectorId = sectorId;
	}

	public String getBlueprintId() {
		return blueprintId;
	}

	public void setBlueprintId(String blueprintId) {
		this.blueprintId = blueprintId;
	}

	public String getStatusId() {
		return statusId;
	}

	public void setStatusId(String statusId) {
		this.statusId = statusId;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public float getPosX() {
		return posX;
	}

	public void setPosX(float posX) {
		this.posX = posX;
	}

	public float getPosY() {
		return posY;
	}

	public void setPosY(float posY) {
		this.posY = posY;
	}

	public float getPosZ() {
		return posZ;
	}

	public void setPosZ(float posZ) {
		this.posZ = posZ;
	}

	public float getRotation() {
		return rotation;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public Integer getChunkX() {
		return chunkX;
	}

	public Integer getChunkY() {
		return chunkY;
	}

	public int getCurrentHealth() {
		return currentHealth;
	}

	public void setCurrentHealth(int currentHealth) {
		this.currentHealth = currentHealth;
	}

	public Map<String, Object> getStateData() {
		return stateData;
	}

	public void setStateData(Map<String, Object> stateData) {
		this.stateData = stateData;
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

	// --- Standard Methods ---

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PlayerEntity that = (PlayerEntity) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "PlayerEntity{" + "id=" + id + ", playerId=" + playerId + ", blueprintId='" + blueprintId + '\''
				+ ", pos=[" + posX + "," + posY + "]" + '}';
	}
}
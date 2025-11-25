package com.cosmic.scavengers.db.meta;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a game entity (like a base or unit) owned by a player.
 */
@Entity
@Table(name = "player_entities")
public class PlayerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "player_id", nullable = false)
	private Long playerId;

	@Column(name = "entity_type", length = 50, nullable = false)
	private String entityType; // e.g., "MAIN_BASE", "DRONE"

	@Column(name = "pos_x", nullable = false)
	private int posX;

	@Column(name = "pos_y", nullable = false)
	private int posY;

	@Column(name = "health", nullable = false)
	private int health;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	/**
	 * Stores complex, dynamic state data for the entity as a JSONB object in
	 * Postgres. The @JdbcTypeCode(SqlTypes.JSON) ensures Hibernate
	 * serializes/deserializes the String content correctly for the JSONB column
	 * type.
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "state_data", columnDefinition = "jsonb")
	private String stateData;

	// --- Constructors ---

	public PlayerEntity() {
	}

	public PlayerEntity(Long playerId, String entityType, int posX, int posY, int health, String stateData) {
		this.playerId = playerId;
		this.entityType = entityType;
		this.posX = posX;
		this.posY = posY;
		this.health = health;
		this.stateData = stateData;
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

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public int getPosX() {
		return posX;
	}

	public void setPosX(int posX) {
		this.posX = posX;
	}

	public int getPosY() {
		return posY;
	}

	public void setPosY(int posY) {
		this.posY = posY;
	}

	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getStateData() {
		return stateData;
	}

	public void setStateData(String stateData) {
		this.stateData = stateData;
	}
}
package com.cosmic.scavengers.db.jpa.domain;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Monitors file state to prevent unnecessary database churn and speed up server
 * boot times.
 */
@Entity
@Table(name = "ingestion_metadata")
public class IngestionMetadata {

	@Id
	@Column(name = "file_path")
	private String filePath;

	@Column(name = "last_hash", nullable = false)
	private String lastHash;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	// Required Default Constructor
	public IngestionMetadata() {
	}

	public IngestionMetadata(String filePath, String lastHash, OffsetDateTime updatedAt) {
		this.filePath = filePath;
		this.lastHash = lastHash;
		this.updatedAt = updatedAt;
	}

	// --- Getters and Setters ---

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getLastHash() {
		return lastHash;
	}

	public void setLastHash(String lastHash) {
		this.lastHash = lastHash;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	// --- Identity & Utility ---

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		IngestionMetadata that = (IngestionMetadata) o;
		return Objects.equals(filePath, that.filePath);
	}

	@Override
	public int hashCode() {
		return Objects.hash(filePath);
	}

	@Override
	public String toString() {
		return "IngestionMetadata{" + "filePath='" + filePath + '\'' + ", lastHash='" + lastHash + '\'' + ", updatedAt="
				+ updatedAt + '}';
	}
}
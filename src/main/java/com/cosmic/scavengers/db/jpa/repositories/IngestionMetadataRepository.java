package com.cosmic.scavengers.db.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cosmic.scavengers.db.jpa.domain.IngestionMetadata;

@Repository
public interface IngestionMetadataRepository extends JpaRepository<IngestionMetadata, String> {
	// Standard CRUD is sufficient to check for file hashes
}
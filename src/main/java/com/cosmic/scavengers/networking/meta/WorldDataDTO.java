package com.cosmic.scavengers.networking.meta;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object (DTO) representing the non-sensitive metadata for a
 * specific game world. This object is a public contract for network
 * communication (client/server) and is explicitly populated by the WorldService
 * layer. * Note: DTOs are immutable and use constructor injection for all
 * fields.
 */
public record WorldDataDTO(
		// World Metadata (Static, from DB)
		@JsonProperty("id") long id,

		@JsonProperty("worldName") String worldName,

		@JsonProperty("mapSeed") long mapSeed,

		@JsonProperty("sectorSizeUnits") int sectorSizeUnits) {
}
package com.cosmic.scavengers.networking.dto;

public class PositionUpdateDto {
	// We assume entity IDs are long in your Dominion ECS setup
	public final String entityName;

	// Fixed-point unscaled values (8 bytes each)
	public final long unscaledX;
	public final long unscaledY;

	public PositionUpdateDto(String entityName, long unscaledX, long unscaledY) {
		this.entityName = entityName;
		this.unscaledX = unscaledX;
		this.unscaledY = unscaledY;
	}
}
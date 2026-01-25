package com.cosmic.scavengers.ecs.domain.components;

import org.decimal4j.api.Decimal;
import org.decimal4j.scale.Scale4f;

import com.cosmic.scavengers.ecs.domain.components.meta.IEcsComponent;

/**
 * Component defining the entity's current location in the game world.
 *
 * Uses Decimal<?> for deterministic fixed-point arithmetic. The specific scale
 * (e.g., Decimal<S4>) must be chosen when instantiated to match the precision
 * agreed upon by the server and client.
 */
public record Position(
		Decimal<Scale4f> x, 
		Decimal<Scale4f> y, 
		Decimal<Scale4f> z) implements IEcsComponent {

	public String toString() {
		return String.format("Position(x=%s, y=%s, z=%s)", x, y, z);
	}
}
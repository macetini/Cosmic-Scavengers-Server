package com.cosmic.scavengers.component;

import org.decimal4j.api.Decimal;
import org.decimal4j.scale.Scale4f;

/**
 * Component defining an entity's movement goal and movement speed.
 *
 * This component is used by the MovementSystem to update the Position.
 */
public record Movement(Decimal<Scale4f> targetX, Decimal<Scale4f> targetY, Decimal<Scale4f> speed) {
}
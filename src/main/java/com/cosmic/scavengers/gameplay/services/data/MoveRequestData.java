package com.cosmic.scavengers.gameplay.services.data;

import org.decimal4j.api.Decimal;
import org.decimal4j.scale.Scale4f;

/**
 * Record class that holds the data that describes a move request.
 */
public record MoveRequestData(
        long entityId,
        Long playerId,
        Decimal<Scale4f> targetX, 
        Decimal<Scale4f> targetY, 
        Decimal<Scale4f> targetZ,
        Decimal<Scale4f> movementSpeed, 
        Decimal<Scale4f> rotationSpeed, 
        Decimal<Scale4f> stoppingDistance) {
}

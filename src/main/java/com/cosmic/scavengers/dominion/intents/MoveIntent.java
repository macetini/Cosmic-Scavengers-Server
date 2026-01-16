package com.cosmic.scavengers.dominion.intents;

import org.decimal4j.api.Decimal;
import org.decimal4j.scale.Scale4f;

import com.cosmic.scavengers.dominion.intents.meta.IEcsIntent;

public record MoveIntent(
		Decimal<Scale4f> targetX, 
		Decimal<Scale4f> targetY, 
		Decimal<Scale4f> targetZ,
		Decimal<Scale4f> speed) implements IEcsIntent {
}
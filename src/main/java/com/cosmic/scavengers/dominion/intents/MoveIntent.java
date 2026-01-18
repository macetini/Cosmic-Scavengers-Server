package com.cosmic.scavengers.dominion.intents;

import com.cosmic.scavengers.dominion.intents.meta.IEcsIntent;
import com.cosmic.scavengers.gameplay.services.entities.data.MoveRequestData;

public record MoveIntent(
		long entityId,
		Long playerId,
		MoveRequestData requsetData) implements IEcsIntent {	
}
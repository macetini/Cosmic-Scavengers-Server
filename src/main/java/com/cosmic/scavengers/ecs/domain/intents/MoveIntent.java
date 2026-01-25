package com.cosmic.scavengers.ecs.domain.intents;

import com.cosmic.scavengers.ecs.domain.intents.meta.IEcsIntent;
import com.cosmic.scavengers.gameplay.services.data.MoveRequestData;

public record MoveIntent(
		long entityId,
		Long playerId,
		MoveRequestData requsetData) implements IEcsIntent {	
}
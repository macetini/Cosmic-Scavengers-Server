package com.cosmic.scavengers.dominion.messaging.meta;

import com.cosmic.scavengers.gameplay.registry.EntityRegistry;
import dev.dominion.ecs.api.Dominion;

public interface IEcsCommand {
	void execute(Dominion dominion, EntityRegistry entityRegistry);
}
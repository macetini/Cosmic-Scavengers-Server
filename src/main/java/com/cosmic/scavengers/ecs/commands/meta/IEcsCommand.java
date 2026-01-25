package com.cosmic.scavengers.ecs.commands.meta;

import com.cosmic.scavengers.registries.EntityRegistry;

import dev.dominion.ecs.api.Dominion;

public interface IEcsCommand {
	void execute(Dominion dominion, EntityRegistry entityRegistry);
}
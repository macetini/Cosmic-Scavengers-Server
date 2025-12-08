package com.cosmic.scavengers.broadcast;

import dev.dominion.ecs.api.Dominion;

/**
 * Interface for the service responsible for collecting and broadcasting the ECS
 * game state (position updates, etc.) as a binary packet.
 */
public interface IStateBroadcaster {
	/**
	 * Collects necessary game state from the Dominion ECS and broadcasts the
	 * compiled binary message to all connected clients.
	 * 
	 * @param dominion the current ECS context containing authoritative state
	 */
	void broadcastCurrentState(Dominion dominion);
}
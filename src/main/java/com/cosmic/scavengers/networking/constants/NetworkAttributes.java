package com.cosmic.scavengers.networking.constants;

import io.netty.util.AttributeKey;

public final class NetworkAttributes {
	/**
	 * The unique database ID of the player associated with this connection.
	 */
	public static final AttributeKey<Long> PLAYER_ID = AttributeKey.valueOf(NetworkAttributeKeys.PLAYER_ID_KEY.name());

	private NetworkAttributes() {
		// Prevent instantiation
		throw new AssertionError("NetworkAttributes cannot be instantiated");
	}
}
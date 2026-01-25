package com.cosmic.scavengers.networking.constants;

import io.netty.util.AttributeKey;

/**
 * The single source of truth for all data attached to a Netty Channel.
 */
public enum NetworkAttributeKeys {
	PLAYER_ID_KEY, 
	SESSION_START_TIME, 
	CLIENT_VERSION;

	/**
	 * Helper to get the actual Netty AttributeKey associated with this enum entry.
	 * We cache it as a field so it's only created once.
	 */
	private final AttributeKey<Object> key = AttributeKey.valueOf(this.name());

	@SuppressWarnings("unchecked")
	public <T> AttributeKey<T> getKey() {
		return (AttributeKey<T>) key;
	}
}
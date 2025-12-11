package com.cosmic.scavengers.networking.commands;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing various network commands used in the Cosmic Scavengers
 * server-client communication.
 */
public enum NetworkBinaryCommands {
	REQUEST_WORLD_STATE_C(0x0001, "Request the current state of the game world."),
	REQUEST_WORLD_STATE_S(0x0002, "Send the current state of the game world."),

	REQUEST_PLAYER_ENTITIES_C(0x0003, "Request the entities associated with a player."),
	REQUEST_PLAYER_ENTITIES_S(0x0004, "Send the entities associated with a player.");

	private static final Map<Short, NetworkBinaryCommands> BY_CODE = new HashMap<>();
	static {
		// This block runs once to populate the map for fast, constant-time lookup.
		for (NetworkBinaryCommands command : NetworkBinaryCommands.values()) {
			BY_CODE.put(command.getCode(), command);
		}
	}

	private final short code;
	private final String description;

	NetworkBinaryCommands(int code, String description) {
		this.code = (short) code;
		this.description = description;
	}

	public static NetworkBinaryCommands fromCode(Short code) {
		return BY_CODE.get(code);
	}

	public short getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Helper for logging to get both the name and the numerical code.
	 */
	public String getLogName() {
		return String.format("name: '%s' - code: '(0x%04X)' - desc: '%s'", this.name(), this.getCode(),
				this.getDescription());
	}
}
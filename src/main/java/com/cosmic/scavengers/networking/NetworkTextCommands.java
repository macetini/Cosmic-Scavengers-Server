package com.cosmic.scavengers.networking;

import java.util.HashMap;
import java.util.Map;

public enum NetworkTextCommands {
	C_CONNECT("C_CONNECT", "Client requests to connect to the server"),
	C_LOGIN("C_LOGIN", "Client requests to log in with credentials"),
	C_REGISTER("C_REGISTER", "Client requests to register a new account");

	private static final Map<String, NetworkTextCommands> BY_CODE = new HashMap<>();
	static {
		// This block runs once to populate the map for fast, constant-time lookup.
		for (NetworkTextCommands command : NetworkTextCommands.values()) {
			BY_CODE.put(command.getCode(), command);
		}
	}

	private final String code;
	private final String description;

	NetworkTextCommands(String code, String description) {
		this.code = code;
		this.description = description;
	}
	
	public static NetworkTextCommands fromCode(String code) {
		return BY_CODE.get(code);
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Helper for logging to get both the name and the numerical code.
	 */
	public String getLogName() {
		return String.format("name: '%s' - code: '%s' - desc: '%s'", this.name(), this.getCode(),
				this.getDescription());
	}

}

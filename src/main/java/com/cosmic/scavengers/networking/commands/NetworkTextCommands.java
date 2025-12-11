package com.cosmic.scavengers.networking.commands;

import java.util.HashMap;
import java.util.Map;

public enum NetworkTextCommands {
	C_CONNECT("C_CONNECT", "Client requests to connect to the server"),
	S_CONNECT_OK("S_CONNECT_OK", "Server acknowledges client connection"),

	C_LOGIN("C_LOGIN", "Client requests to log in with credentials"),
	S_LOGIN_OK("S_LOGIN_OK", "Server responds to client login attempt"),
	S_LOGIN_FAIL("S_LOGIN_FAIL", "Server informs client of failed login attempt"),

	C_REGISTER("C_REGISTER", "Client requests to register a new account"),
	S_REGISTER_OK("S_REGISTER_OK", "Server responds to client registration attempt"),
	S_REGISTER_FAIL("S_REGISTER_FAIL", "Server informs client of failed registration attempt");

	private static final Map<String, NetworkTextCommands> BY_CODE = new HashMap<>();
	static {		
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

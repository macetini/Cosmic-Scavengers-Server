package com.cosmic.scavengers.networking.commands.router.meta;

public enum CommandType {
	TYPE_UNKNOWN((byte) 0x00), TYPE_TEXT((byte) 0x01), TYPE_BINARY((byte) 0x02);

	private final byte value;

	private static final CommandType[] LOOKUP = new CommandType[3];

	static {
		for (CommandType type : values()) {
			if (type.value >= 0 && type.value < LOOKUP.length) {
				LOOKUP[type.value] = type;
			}
		}
	}

	CommandType(byte value) {
		this.value = value;
	}

	public byte getValue() {
		return value;
	}

	public static CommandType fromValue(byte value) {
		if (value < 0 || value >= LOOKUP.length) {
			return TYPE_UNKNOWN;
		}
		CommandType type = LOOKUP[value];
		return type != null ? type : TYPE_UNKNOWN;
	}
}
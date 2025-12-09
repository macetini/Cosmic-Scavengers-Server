package com.cosmic.scavengers.networking.requests.handlers.meta;

public enum PacketType {
	TYPE_TEXT((byte) 0x01), TYPE_BINARY((byte) 0x02);
	
	private final byte value;

	PacketType(byte value) {
		this.value = value;
	}

	public byte getValue() {
		return value;
	}
}
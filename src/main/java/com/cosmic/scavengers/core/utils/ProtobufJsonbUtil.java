package com.cosmic.scavengers.core.utils;

import org.jooq.JSONB;
import org.slf4j.Logger;

/**
 * Utility class for converting between jOOQ's JSONB type (used in database
 * POJOs) and a standard Java String (used for Protobuf serialization).
 */
public final class ProtobufJsonbUtil {
	private static Logger log = org.slf4j.LoggerFactory.getLogger(ProtobufJsonbUtil.class);

	private ProtobufJsonbUtil() {
		// Utility class
	}

	/**
	 * 
	 * Converts a jOOQ JSONB object to its String representation. This String is
	 * then ready to be set in the Protobuf message field.
	 * 
	 * @param jsonb The source JSONB object from the POJO (can be null).
	 * 
	 * @return The raw JSON string content (or null if input is null).
	 * 
	 */
	public static String toJsonString(JSONB jsonb) {
		if (jsonb == null) {
			log.warn("JSONB object is null, returning empty JSON string");
			return "";
		}
		return jsonb.data();
	}

	/**
	 * 
	 * Converts a JSON String received from a Protobuf message back into a jOOQ
	 * JSONB object for use in the database POJO.
	 * 
	 * @param jsonString The raw JSON string (can be null or empty).
	 * 
	 * @return The corresponding jOOQ JSONB object (or null if input is null/empty).
	 * 
	 */
	public static JSONB fromJsonString(String jsonString) {
		if (jsonString == null || jsonString.trim().isEmpty()) {
			log.warn("JSON string is null or empty, returning empty JSONB object");
			return JSONB.valueOf("{}");
		}
		return JSONB.valueOf(jsonString);
	}
}

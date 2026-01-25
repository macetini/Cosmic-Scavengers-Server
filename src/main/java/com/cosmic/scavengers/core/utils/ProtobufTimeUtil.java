package com.cosmic.scavengers.core.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Timestamp;

/**
 * 
 * Utility class for converting between Java's modern date/time types
 * (java.time) and Google's Protocol Buffers Timestamp.
 * 
 */
public final class ProtobufTimeUtil {
	private static final Logger log = LoggerFactory.getLogger(ProtobufTimeUtil.class);
	
	private ProtobufTimeUtil() {
		// Utility class
	}
	/**
	 * 
	 * Converts a java.time.OffsetDateTime to a com.google.protobuf.Timestamp. The
	 * OffsetDateTime is converted to UTC Instant before mapping to Timestamp
	 * seconds/nanos.
	 * 
	 * @param offsetDateTime The source OffsetDateTime (can be null).
	 * 
	 * @return The corresponding Protobuf Timestamp (or null if input is null).
	 * 
	 */
	public static Timestamp toProtobufTimestamp(OffsetDateTime offsetDateTime) {
		if (offsetDateTime == null) {
			log.warn("OffsetDateTime is null, returning null Timestamp");
			return null;
		}

		Instant instant = offsetDateTime.toInstant();
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}

	/**
	 * Converts a com.google.protobuf.Timestamp back to a java.time.OffsetDateTime.
	 * * @param timestamp The source Protobuf Timestamp (can be null).
	 * 
	 * @return The corresponding OffsetDateTime in UTC (ZoneOffset.UTC) (or null if
	 *         input is null).
	 */
	public static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
		if (timestamp == null || timestamp.getSeconds() == 0 && timestamp.getNanos() == 0) {
			log.warn("Protobuf Timestamp is null or zero, returning null OffsetDateTime");
			return null;
		}
		
		// 1. Recreate the Instant from Protobuf seconds and nanoseconds
		Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
		
		// 2. Convert the Instant to an OffsetDateTime, usually anchored to UTC for
		// consistency
		return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
package com.cosmic.scavengers.util;

import java.math.RoundingMode;

import org.decimal4j.api.Decimal;
import org.decimal4j.api.DecimalArithmetic;
import org.decimal4j.factory.DecimalFactory;
import org.decimal4j.factory.Factories;
import org.decimal4j.scale.Scale4f;

import com.cosmic.scavengers.proto.GameMessages.FixedPoint;
import com.cosmic.scavengers.util.meta.GameDecimal;

/**
 * Utility class to manage fixed-point number conversions using Decimal4j. The
 * game uses Scale4f (S4) for 4 decimal places of precision, ensuring
 * deterministic results across all clients and the server.
 */
public final class DecimalUtils {

	// 1. Initialize the ARITHMETIC instance first, using the required RoundingMode
	public static final DecimalArithmetic ARITHMETIC = Scale4f.INSTANCE.getArithmetic(RoundingMode.HALF_EVEN);

	// 2. Create the DecimalFactory using the defined Scale and Arithmetic
	public static final DecimalFactory<Scale4f> FACTORY = Factories.getDecimalFactory(Scale4f.INSTANCE);

	private DecimalUtils() {
		// Prevent instantiation
	}

	/**
	 * Converts a Protobuf FixedPoint message (long unscaled value) into a Decimal4j
	 * object.
	 * 
	 * @param fixedPoint The Protobuf message containing the unscaled value.
	 * @return The Decimal4j representation.
	 */
	public static Decimal<Scale4f> toDecimal(FixedPoint fixedPoint) {
		return FACTORY.valueOf(fixedPoint.getValue());
	}

	/**
	 * Converts a Decimal4j object back into a Protobuf FixedPoint message.
	 * 
	 * @param decimal The Decimal4j object.
	 * @return The Protobuf FixedPoint message.
	 */
	public static FixedPoint toFixedPoint(Decimal<Scale4f> decimal) {
		return FixedPoint.newBuilder().setValue(decimal.unscaledValue()).build();
	}

	/**
	 * Creates a Decimal4j object from a standard double for initial setup only.
	 * This should not be used in the main game loop to avoid floating point errors.
	 */
	public static Decimal<Scale4f> fromDouble(double value) {
		return FACTORY.valueOf(value);
	}

	/**
	 * Creates a GameDecimal instance from an unscaled long value. This encapsulates
	 * the required casting and ensures consistency. 
	 *
	 * @param unscaledValue the unscaled long value (Scale4f).
	 * 
	 * @return a new GameDecimal instance.
	 */	
	public static GameDecimal fromUnscaled(long unscaledValue) {
		return (GameDecimal) FACTORY.valueOf(unscaledValue);
	}
}
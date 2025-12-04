package com.cosmic.scavengers.util;

import java.math.RoundingMode;

import org.decimal4j.api.Decimal;
import org.decimal4j.api.DecimalArithmetic;
import org.decimal4j.factory.DecimalFactory;
import org.decimal4j.factory.Factories;
import org.decimal4j.scale.Scale4f;

/**
 * Utility class to manage fixed-point number conversions using Decimal4j. The
 * game uses Scale4f (S4) for 4 decimal places of precision, ensuring
 * deterministic results across all clients and the server.
 */
public final class DecimalUtils {

	public static final DecimalArithmetic ARITHMETIC = Scale4f.INSTANCE.getArithmetic(RoundingMode.HALF_EVEN);

	public static final DecimalFactory<Scale4f> FACTORY = Factories.getDecimalFactory(Scale4f.INSTANCE);

	private DecimalUtils() {
		// Prevent instantiation
	}

	/**
	 * Converts an unscaled long value (from protobuf) into a Decimal4j object.
	 */
	public static Decimal<Scale4f> fromUnscaled(long unscaledValue) {
		return FACTORY.valueOfUnscaled(unscaledValue);
	}

	/**
	 * Converts a Decimal4j object back into an unscaled long value suitable for
	 * protobuf fields.
	 */
	public static long toUnscaled(Decimal<Scale4f> decimal) {
		return decimal.unscaledValue();
	}

	/**
	 * Creates a Decimal4j object from a standard double. Should not be used in the
	 * main game loop.
	 */
	public static Decimal<Scale4f> fromDouble(double value) {
		return FACTORY.valueOf(value);
	}

	/**
	 * Creates a Decimal4j object from a long integer value.
	 */
	public static Decimal<Scale4f> fromInteger(long value) {
		return FACTORY.valueOf(value);
	}
}

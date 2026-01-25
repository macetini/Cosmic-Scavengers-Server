package com.cosmic.scavengers.core.utils;

import java.math.RoundingMode;
import org.decimal4j.api.Decimal;
import org.decimal4j.api.DecimalArithmetic;
import org.decimal4j.factory.DecimalFactory;
import org.decimal4j.factory.Factories;
import org.decimal4j.scale.Scale4f;

/**
 * Utility class to manage fixed-point number conversions using Decimal4j.
 * Ensures deterministic results across all clients and the server. Naming and
 * rounding match the C# DeterministicUtils for cross-platform symmetry.
 */
public final class DecimalUtils {

	/**
	 * Arithmetic used for all Decimal4j operations. Note: Using HALF_UP (Normal
	 * rounding) to match C# MidpointRounding.AwayFromZero.
	 */
	public static final DecimalArithmetic ARITHMETIC = Scale4f.INSTANCE.getArithmetic(RoundingMode.HALF_UP);

	/**
	 * Factory for creating Scale4f decimal instances.
	 */
	public static final DecimalFactory<Scale4f> FACTORY = Factories.getDecimalFactory(Scale4f.INSTANCE);

	private DecimalUtils() {
		// Prevent instantiation
		throw new AssertionError("DecimalUtils cannot be instantiated");
	}

	// ==============================================================
	// TO SCALED (Encoding to long: Value * 10,000)
	// ==============================================================

	/**
	 * Converts (Encodes) a Decimal4j object into a scaled long value (Value *
	 * 10,000).
	 */
	public static long toScaled(Decimal<Scale4f> decimal) {
		return decimal.unscaledValue();
	}

	/**
	 * Converts a float (e.g. from Database) directly to a scaled long.
	 */
	public static long toScaled(float value) {
		return FACTORY.valueOf(value).unscaledValue();
	}

	/**
	 * Converts a boxed Float (possibly null) to a scaled long.
	 * 
	 * @returns 0 if the input is null.
	 */
	public static long toScaled(Float value) {
		if (value == null)
			return 0L;
		return FACTORY.valueOf(value.floatValue()).unscaledValue();
	}

	/**
	 * Converts a double (e.g. from Config) directly to a scaled long.
	 */
	public static long toScaled(double value) {
		return FACTORY.valueOf(value).unscaledValue();
	}

	/**
	 * Converts a boxed Double (possibly null) to a scaled long. Returns 0 if the
	 * input is null.
	 * 
	 * @returns 0 if the input is null.
	 */
	public static long toScaled(Double value) {
		if (value == null)
			return 0L;
		return FACTORY.valueOf(value.doubleValue()).unscaledValue();
	}

	// ==============================================================
	// FROM SCALED / VALUE CREATION (Decoding to Decimal object)
	// ==============================================================

	/** From Network/Database long */
	public static Decimal<Scale4f> fromScaled(long scaledValue) {
		return FACTORY.valueOfUnscaled(scaledValue);
	}

	/** From primitive float */
	public static Decimal<Scale4f> fromScaled(float value) {
		return FACTORY.valueOf(value);
	}

	/** From boxed Float (Prevents NPE in ECS spawning) */
	public static Decimal<Scale4f> fromScaled(Float value) {
		if (value == null)
			return FACTORY.valueOf(0);
		return FACTORY.valueOf(value.floatValue());
	}

	/** From primitive double */
	public static Decimal<Scale4f> fromScaled(double value) {
		return FACTORY.valueOf(value);
	}

	/** From boxed Double */
	public static Decimal<Scale4f> fromScaled(Double value) {
		if (value == null)
			return FACTORY.valueOf(0);
		return FACTORY.valueOf(value.doubleValue());
	}
}
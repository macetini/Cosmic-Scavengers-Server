package com.cosmic.scavengers.system;

import static com.cosmic.scavengers.util.DecimalUtils.ARITHMETIC;

import org.decimal4j.api.Decimal;
import org.decimal4j.scale.Scale4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.component.Movement;
import com.cosmic.scavengers.component.Position;
import com.cosmic.scavengers.util.DecimalUtils;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * Handles the movement of all entities. Implements Runnable and queries the
 * Dominion context directly, which is the correct pattern for systems in the
 * modern Dominion ECS API.
 */
public class MovementSystem implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(MovementSystem.class);

	// Time delta for fixed-point math (0.1 seconds per tick)
	private static final Decimal<Scale4f> TICK_DELTA = DecimalUtils.fromDouble(0.1);
	// Distance threshold for snapping to target
	private static final Decimal<Scale4f> THRESHOLD = DecimalUtils.fromDouble(0.0001);

	// Precomputed threshold squared in unscaled long form
	// To ensure 100% determinism and API compatibility, using ARITHMETIC instance
	// instead of power function
	private static final long THRESHOLD_SQUARED_UNSCALED = ARITHMETIC.multiply(THRESHOLD.unscaledValue(),
			THRESHOLD.unscaledValue());

	private final Dominion dominion;

	public MovementSystem(Dominion dominion) {
		this.dominion = dominion;
	}

	public static class DistanceDelta {
		Decimal<Scale4f> deltaX;
		Decimal<Scale4f> deltaY;

		DistanceDelta(Decimal<Scale4f> deltaX, Decimal<Scale4f> deltaY) {
			this.deltaX = deltaX;
			this.deltaY = deltaY;
		}
	}

	public static class NormalizedDirection {
		long normXUnscaled;
		long normYUnscaled;

		NormalizedDirection(long normXUnscaled, long normYUnscaled) {
			this.normXUnscaled = normXUnscaled;
			this.normYUnscaled = normYUnscaled;
		}
	}

	public static class DisplacementVector {
		long dispXUnscaled;
		long dispYUnscaled;

		DisplacementVector(long dispXUnscaled, long dispYUnscaled) {
			this.dispXUnscaled = dispXUnscaled;
			this.dispYUnscaled = dispYUnscaled;
		}
	}

	@Override
	public void run() {
		// Query entities with both Position and Movement components
		dominion.findEntitiesWith(Position.class, Movement.class).stream().forEach(result -> {
			try {
				processMovementTick(result.entity(), result.comp1(), result.comp2());
			} catch (Exception e) {
				// Log the exception to aid in debugging runtime failures during ECS loop
				// execution
				log.error("Error processing movement for entity {}.", result.entity().getName(), e);
			}
		});
	}

	private void processMovementTick(final Entity entity, final Position position, final Movement movement) {
		// Calculate Distance Delta = Target Position - Current Position
		DistanceDelta distanceDelta = calculateDistanceDelta(position, movement);

		// Calculate Distance Squared (unscaled) using Pythagoras
		long distanceSquaredUnscaled = calculateDistanceSquaredUnscaled(distanceDelta);

		// Compute distance (unscaled)
		long distanceUnscaled = ARITHMETIC.sqrt(distanceSquaredUnscaled);

		// Displacement Magnitude (DM) = Speed * Time Delta
		long displacementUnscaled = ARITHMETIC.multiply(movement.speed().unscaledValue(), TICK_DELTA.unscaledValue());

		final long absoluteDisplacement = Math.abs(displacementUnscaled);

		// Check Snap Condition: Reached target, distance is zero, or overshot target
		boolean reachedTarget = distanceSquaredUnscaled <= THRESHOLD_SQUARED_UNSCALED;
		boolean distanceUnscaledIsZero = distanceUnscaled == 0L;
		boolean overshotTarget = absoluteDisplacement >= distanceUnscaled;

		if (reachedTarget || distanceUnscaledIsZero || overshotTarget) {
			handleSnapCondition(entity, movement);
			return;
		}

		// Normalized Direction Vector (NDV) = Delta / Distance
		NormalizedDirection normalizedDirection = calculateNormalizedDirection(distanceUnscaled, distanceDelta);

		// Displacement Vector = (NDV * DM)
		DisplacementVector displacementVector = calculateDisplacementVector(displacementUnscaled, normalizedDirection);

		// New Position = Current Position + Displacement Vector
		Position newPosition = calculateNewPosition(position, displacementVector);
		entity.add(newPosition);
	}

	private DistanceDelta calculateDistanceDelta(Position position, Movement movement) {
		// Delta = Target - Current
		Decimal<Scale4f> deltaX = movement.targetX().subtract(position.x());
		Decimal<Scale4f> deltaY = movement.targetY().subtract(position.y());

		return new DistanceDelta(deltaX, deltaY);
	}

	private long calculateDistanceSquaredUnscaled(DistanceDelta distanceDelta) {
		// Distance Squared = (DeltaX^2) + (DeltaY^2)
		long deltaXSquaredUnscaled = ARITHMETIC.multiply(distanceDelta.deltaX.unscaledValue(),
				distanceDelta.deltaX.unscaledValue());
		long deltaYSquaredUnscaled = ARITHMETIC.multiply(distanceDelta.deltaY.unscaledValue(),
				distanceDelta.deltaY.unscaledValue());

		return ARITHMETIC.add(deltaXSquaredUnscaled, deltaYSquaredUnscaled);
	}

	private void handleSnapCondition(Entity entity, Movement movement) {
		Position finalPosition = new Position(movement.targetX(), movement.targetY());

		entity.add(finalPosition);
		entity.removeType(Movement.class);
	}

	private NormalizedDirection calculateNormalizedDirection(long distanceUnscaled, DistanceDelta distanceDelta) {
		long normXUnscaled = ARITHMETIC.divide(distanceDelta.deltaX.unscaledValue(), distanceUnscaled);
		long normYUnscaled = ARITHMETIC.divide(distanceDelta.deltaY.unscaledValue(), distanceUnscaled);

		return new NormalizedDirection(normXUnscaled, normYUnscaled);
	}

	private DisplacementVector calculateDisplacementVector(long displacementUnscaled,
			NormalizedDirection normalizedDirection) {

		long dispXUnscaled = ARITHMETIC.multiply(normalizedDirection.normXUnscaled, displacementUnscaled);
		long dispYUnscaled = ARITHMETIC.multiply(normalizedDirection.normYUnscaled, displacementUnscaled);

		return new DisplacementVector(dispXUnscaled, dispYUnscaled);
	}

	private Position calculateNewPosition(Position position, DisplacementVector displacementVector) {
		long newX = ARITHMETIC.add(position.x().unscaledValue(), displacementVector.dispXUnscaled);
		long newY = ARITHMETIC.add(position.y().unscaledValue(), displacementVector.dispYUnscaled);

		return new Position(DecimalUtils.fromUnscaled(newX), DecimalUtils.fromUnscaled(newY));
	}
}
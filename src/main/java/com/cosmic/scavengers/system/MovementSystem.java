package com.cosmic.scavengers.system;

import static com.cosmic.scavengers.utils.DecimalUtils.ARITHMETIC;

import org.decimal4j.api.Decimal;
import org.decimal4j.scale.Scale4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.dominion.components.Movement;
import com.cosmic.scavengers.dominion.components.Position;
import com.cosmic.scavengers.dominion.intents.MoveIntent;
import com.cosmic.scavengers.utils.DecimalUtils;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * Handles movement for entities in the ECS world.
 *
 * <p>
 * This system performs deterministic, fixed-timestep movement using fixed-point
 * arithmetic (Decimal4j) to avoid floating point nondeterminism. It queries the
 * provided {@link Dominion} instance for entities that have both
 * {@link Position} and {@link Movement} components and advances their positions
 * by a single tick amount when {@link #run()} is invoked.
 *
 * <p>
 * Movement logic overview:
 * <ul>
 * <li>Compute the vector from current position to target position.</li>
 * <li>If the distance is within a small threshold, or the displacement for this
 * tick would overshoot the target, the entity is snapped to the target and its
 * {@link Movement} component is removed.</li>
 * <li>Otherwise, compute a normalized direction (using unscaled long values),
 * scale it by the displacement magnitude (speed * tick delta), and add the
 * resulting displacement to the current position.</li>
 * </ul>
 *
 * <p>
 * Design notes:
 * <ul>
 * <li>The system uses precomputed unscaled values and the DecimalUtils
 * arithmetic instance to preserve determinism across platforms.</li>
 * <li>Tick delta and snapping threshold are stored as Decimal&lt;Scale4f&gt;
 * constants and squared threshold is cached in unscaled form.</li>
 * </ul>
 */
@Component
public class MovementSystem implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(MovementSystem.class);

	// Time delta for fixed-point math (0.1 seconds per tick)
	private static final Decimal<Scale4f> TICK_DELTA = DecimalUtils.fromScaled(1000L);
	// Distance threshold for snapping to target
	private static final Decimal<Scale4f> THRESHOLD = DecimalUtils.fromScaled(1L);

	// Precomputed threshold squared in unscaled long form
	// To ensure 100% determinism and API compatibility, using ARITHMETIC instance
	// instead of power function
	private static final long THRESHOLD_SQUARED_UNSCALED = ARITHMETIC.multiply(THRESHOLD.unscaledValue(),
			THRESHOLD.unscaledValue());

	private final Dominion dominion;

	/**
	 * Creates a new MovementSystem that will query the supplied Dominion for
	 * entities to process.
	 *
	 * @param dominion the ECS context used to find entities with Position and
	 *                 Movement components (must not be null)
	 */
	public MovementSystem(Dominion dominion) {
		this.dominion = dominion;
	}

	/**
	 * Simple holder for the difference between target and current position in X/Y
	 * as fixed-point decimals.
	 *
	 * <p>
	 * Fields are package-private because they are only used internally by the
	 * movement computations; this class exists to keep related values grouped and
	 * improve readability of the algorithmic steps.
	 * </p>
	 */
	public static class DistanceDelta {
		Decimal<Scale4f> deltaX;
		Decimal<Scale4f> deltaY;
		Decimal<Scale4f> deltaZ;

		DistanceDelta(Decimal<Scale4f> deltaX, Decimal<Scale4f> deltaY, Decimal<Scale4f> deltaZ) {
			this.deltaX = deltaX;
			this.deltaY = deltaY;
			this.deltaZ = deltaZ;
		}
	}

	/**
	 * Represents a normalized direction vector using unscaled long values.
	 *
	 * <p>
	 * Normalization is performed using unscaled long arithmetic to preserve
	 * determinism; consumers should treat the fields as fixed-point unscaled values
	 * (matching Decimal.unscaledValue()).
	 * </p>
	 */
	public static class NormalizedDirection {
		long normXUnscaled;
		long normYUnscaled;
		long normZUnscaled;

		NormalizedDirection(long normXUnscaled, long normYUnscaled, long normZUnscaled) {
			this.normXUnscaled = normXUnscaled;
			this.normYUnscaled = normYUnscaled;
			this.normZUnscaled = normZUnscaled;
		}
	}

	/**
	 * Represents the displacement vector to be applied to the position for a single
	 * tick, expressed in unscaled long fixed-point units.
	 */
	public static class DisplacementVector {
		long dispXUnscaled;
		long dispYUnscaled;
		long dispZUnscaled;

		DisplacementVector(long dispXUnscaled, long dispYUnscaled, long dispZUnscaled) {
			this.dispXUnscaled = dispXUnscaled;
			this.dispYUnscaled = dispYUnscaled;
			this.dispZUnscaled = dispZUnscaled;
		}
	}

	/**
	 * Execute a single movement tick.
	 *
	 * <p>
	 * This method will iterate all entities with {@link Position} and
	 * {@link Movement} components and attempt to advance them toward their
	 * configured targets. Any runtime exceptions thrown while processing a specific
	 * entity are caught and logged so that a failure for one entity doesn't stop
	 * the whole system from running.
	 */
	@Override
	public void run() {
		// Query entities with both Position and Movement components
		dominion.findEntitiesWith(MoveIntent.class).stream().forEach(result -> {
			try {
				processMovementTick(result.entity());
			} catch (Exception e) {
				// Log the exception to aid in debugging runtime failures during ECS loop
				// execution
				log.error("Error processing movement for entity {}.", result.entity().getName(), e);
			}
		});
	}

	/**
	 * Perform the movement update for a single entity for this tick.
	 *
	 * @param entity   the entity being updated
	 * @param position the entity's current Position component
	 * @param movement the entity's Movement component (contains target and speed)
	 */
	private void processMovementTick(final Entity entity) {
		final Position position = entity.get(Position.class);

		log.debug("Processing movement for entity {} at {}", entity.getName(), position);
//		
//
//		final Movement movement = entity.get(Movement.class);
//
//		// Calculate Distance Delta = Target Position - Current Position
//		final DistanceDelta distanceDelta = calculateDistanceDelta(position, movement);
//
//		// Calculate Distance Squared (unscaled) using Pythagoras (x^2 + y^2 + z^2)
//		final long distanceSquaredUnscaled = calculateDistanceSquaredUnscaled(distanceDelta);
//
//		// Compute distance (unscaled)
//		final long distanceUnscaled = ARITHMETIC.sqrt(distanceSquaredUnscaled);
//
//		// Displacement Magnitude (DM) = Speed * Time Delta
//		final long displacementUnscaled = ARITHMETIC.multiply(movement.speed().unscaledValue(),
//				TICK_DELTA.unscaledValue());
//
//		// Snap if within threshold or if we would overshoot
//		if (distanceSquaredUnscaled <= THRESHOLD_SQUARED_UNSCALED
//				|| distanceUnscaled <= Math.abs(displacementUnscaled)) {
//			handleSnapCondition(entity, movement);
//			return;
//		}
//
//		// Normalized Direction Vector (NDV) = Delta / Distance
//		final NormalizedDirection normalizedDirection = calculateNormalizedDirection(distanceUnscaled, distanceDelta);
//
//		// Displacement Vector = NDV * Displacement Magnitude
//		final DisplacementVector displacementVector = calculateDisplacementVector(displacementUnscaled,
//				normalizedDirection);
//
//		// New Position = Current Position + Displacement Vector
//		final Position newPosition = calculateNewPosition(position, displacementVector);
//		entity.add(newPosition);
	}

	/**
	 * Compute the difference between the movement target and the current position
	 * in X, Y, and Z (target - current).
	 */
	private DistanceDelta calculateDistanceDelta(Position position, Movement movement) {
		// Delta = Target - Current
		final Decimal<Scale4f> deltaX = movement.targetX().subtract(position.x());
		final Decimal<Scale4f> deltaY = movement.targetY().subtract(position.y());
		final Decimal<Scale4f> deltaZ = movement.targetZ().subtract(position.z());

		return new DistanceDelta(deltaX, deltaY, deltaZ);
	}

	/**
	 * Compute the unscaled squared distance between two points in 3D.
	 */
	private long calculateDistanceSquaredUnscaled(DistanceDelta distanceDelta) {
		final long deltaXSquaredUnscaled = ARITHMETIC.multiply(distanceDelta.deltaX.unscaledValue(),
				distanceDelta.deltaX.unscaledValue());

		final long deltaYSquaredUnscaled = ARITHMETIC.multiply(distanceDelta.deltaY.unscaledValue(),
				distanceDelta.deltaY.unscaledValue());

		final long deltaZSquaredUnscaled = ARITHMETIC.multiply(distanceDelta.deltaZ.unscaledValue(),
				distanceDelta.deltaZ.unscaledValue());

		final long xySumUnscaled = ARITHMETIC.add(deltaXSquaredUnscaled, deltaYSquaredUnscaled);
		return ARITHMETIC.add(xySumUnscaled, deltaZSquaredUnscaled);
	}

	/**
	 * Snap the entity to its movement target and remove the Movement component.
	 */
	private void handleSnapCondition(Entity entity, Movement movement) {
		entity.remove(Movement.class);
		entity.remove(Position.class);

		final Position finalPosition = new Position(movement.targetX(), movement.targetY(), movement.targetZ());
		entity.add(finalPosition);
	}

	/**
	 * Normalize the delta vector by the provided unscaled distance using unscaled
	 * arithmetic.
	 */
	private NormalizedDirection calculateNormalizedDirection(long distanceUnscaled, DistanceDelta distanceDelta) {
		final long normXUnscaled = ARITHMETIC.divide(distanceDelta.deltaX.unscaledValue(), distanceUnscaled);
		final long normYUnscaled = ARITHMETIC.divide(distanceDelta.deltaY.unscaledValue(), distanceUnscaled);
		final long normZUnscaled = ARITHMETIC.divide(distanceDelta.deltaZ.unscaledValue(), distanceUnscaled);

		return new NormalizedDirection(normXUnscaled, normYUnscaled, normZUnscaled);
	}

	/**
	 * Scale the normalized direction by the displacement magnitude.
	 */
	private DisplacementVector calculateDisplacementVector(long displacementUnscaled,
			NormalizedDirection normalizedDirection) {
		final long dispXUnscaled = ARITHMETIC.multiply(normalizedDirection.normXUnscaled, displacementUnscaled);
		final long dispYUnscaled = ARITHMETIC.multiply(normalizedDirection.normYUnscaled, displacementUnscaled);
		final long dispZUnscaled = ARITHMETIC.multiply(normalizedDirection.normZUnscaled, displacementUnscaled);

		return new DisplacementVector(dispXUnscaled, dispYUnscaled, dispZUnscaled);
	}

	/**
	 * Calculate the new position by applying the displacement vector.
	 */
	private Position calculateNewPosition(Position position, DisplacementVector displacementVector) {
		final long newX = ARITHMETIC.add(position.x().unscaledValue(), displacementVector.dispXUnscaled);
		final long newY = ARITHMETIC.add(position.y().unscaledValue(), displacementVector.dispYUnscaled);
		final long newZ = ARITHMETIC.add(position.z().unscaledValue(), displacementVector.dispZUnscaled);

		return new Position(DecimalUtils.fromScaled(newX), DecimalUtils.fromScaled(newY),
				DecimalUtils.fromScaled(newZ));
	}
}
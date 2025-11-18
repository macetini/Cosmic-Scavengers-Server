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
 * Handles movement for entities in the ECS world.
 *
 * <p>This system performs deterministic, fixed-timestep movement using fixed-point
 * arithmetic (Decimal4j) to avoid floating point nondeterminism. It queries the
 * provided {@link Dominion} instance for entities that have both {@link Position}
 * and {@link Movement} components and advances their positions by a single tick
 * amount when {@link #run()} is invoked.
 *
 * <p>Movement logic overview:
 * <ul>
 *   <li>Compute the vector from current position to target position.</li>
 *   <li>If the distance is within a small threshold, or the displacement for
 *       this tick would overshoot the target, the entity is snapped to the target
 *       and its {@link Movement} component is removed.</li>
 *   <li>Otherwise, compute a normalized direction (using unscaled long values),
 *       scale it by the displacement magnitude (speed * tick delta), and add the
 *       resulting displacement to the current position.</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>The system uses precomputed unscaled values and the DecimalUtils
 *       arithmetic instance to preserve determinism across platforms.</li>
 *   <li>Tick delta and snapping threshold are stored as Decimal&lt;Scale4f&gt;
 *       constants and squared threshold is cached in unscaled form.</li>
 * </ul>
 */
public class MovementSystem implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(MovementSystem.class);

	// Time delta for fixed-point math (0.1 seconds per tick)
	private static final Decimal<Scale4f> TICK_DELTA = DecimalUtils.fromUnscaled(1000L);
	// Distance threshold for snapping to target
	private static final Decimal<Scale4f> THRESHOLD = DecimalUtils.fromUnscaled(1L);

	// Precomputed threshold squared in unscaled long form
	// To ensure 100% determinism and API compatibility, using ARITHMETIC instance
	// instead of power function
	private static final long THRESHOLD_SQUARED_UNSCALED = ARITHMETIC.multiply(THRESHOLD.unscaledValue(),
			THRESHOLD.unscaledValue());

	private final Dominion dominion;

	/**
	 * Creates a new MovementSystem that will query the supplied Dominion
	 * for entities to process.
	 *
	 * @param dominion the ECS context used to find entities with Position and
	 *                 Movement components (must not be null)
	 */
	public MovementSystem(Dominion dominion) {
		this.dominion = dominion;
	}

	/**
	 * Simple holder for the difference between target and current position in
	 * X/Y as fixed-point decimals.
	 *
	 * <p>Fields are package-private because they are only used internally by the
	 * movement computations; this class exists to keep related values grouped and
	 * improve readability of the algorithmic steps.
	 */
	public static class DistanceDelta {
		Decimal<Scale4f> deltaX;
		Decimal<Scale4f> deltaY;

		DistanceDelta(Decimal<Scale4f> deltaX, Decimal<Scale4f> deltaY) {
			this.deltaX = deltaX;
			this.deltaY = deltaY;
		}
	}

	/**
	 * Represents a normalized direction vector using unscaled long values.
	 *
	 * <p>Normalization is performed using unscaled long arithmetic to preserve
	 * determinism; consumers should treat the fields as fixed-point unscaled
	 * values (matching Decimal.unscaledValue()).
	 */
	public static class NormalizedDirection {
		long normXUnscaled;
		long normYUnscaled;

		NormalizedDirection(long normXUnscaled, long normYUnscaled) {
			this.normXUnscaled = normXUnscaled;
			this.normYUnscaled = normYUnscaled;
		}
	}

	/**
	 * Represents the displacement vector to be applied to the position for a
	 * single tick, expressed in unscaled long fixed-point units.
	 */
	public static class DisplacementVector {
		long dispXUnscaled;
		long dispYUnscaled;

		DisplacementVector(long dispXUnscaled, long dispYUnscaled) {
			this.dispXUnscaled = dispXUnscaled;
			this.dispYUnscaled = dispYUnscaled;
		}
	}

	/**
	 * Execute a single movement tick.
	 *
	 * <p>This method will iterate all entities with {@link Position} and
	 * {@link Movement} components and attempt to advance them toward their
	 * configured targets. Any runtime exceptions thrown while processing a
	 * specific entity are caught and logged so that a failure for one entity
	 * doesn't stop the whole system from running.
	 */
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

	/**
	 * Perform the movement update for a single entity for this tick.
	 *
	 * @param entity   the entity being updated
	 * @param position the entity's current Position component
	 * @param movement the entity's Movement component (contains target and speed)
	 */
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

	/**
	 * Compute the difference between the movement target and the current
	 * position in X and Y (target - current).
	 */
	private DistanceDelta calculateDistanceDelta(Position position, Movement movement) {
		// Delta = Target - Current
		Decimal<Scale4f> deltaX = movement.targetX().subtract(position.x());
		Decimal<Scale4f> deltaY = movement.targetY().subtract(position.y());

		return new DistanceDelta(deltaX, deltaY);
	}

	/**
	 * Compute the unscaled squared distance between two points given their
	 * delta in fixed-point decimals.
	 */
	private long calculateDistanceSquaredUnscaled(DistanceDelta distanceDelta) {
		// Distance Squared = (DeltaX^2) + (DeltaY^2)
		long deltaXSquaredUnscaled = ARITHMETIC.multiply(distanceDelta.deltaX.unscaledValue(),
				distanceDelta.deltaX.unscaledValue());
		long deltaYSquaredUnscaled = ARITHMETIC.multiply(distanceDelta.deltaY.unscaledValue(),
				distanceDelta.deltaY.unscaledValue());

		return ARITHMETIC.add(deltaXSquaredUnscaled, deltaYSquaredUnscaled);
	}

	/**
	 * Snap the entity to its movement target and remove the Movement component.
	 */
	private void handleSnapCondition(Entity entity, Movement movement) {
		Position finalPosition = new Position(movement.targetX(), movement.targetY());

		entity.add(finalPosition);
		entity.removeType(Movement.class);
	}

	/**
	 * Normalize the delta vector by the provided unscaled distance using
	 * unscaled arithmetic.
	 */
	private NormalizedDirection calculateNormalizedDirection(long distanceUnscaled, DistanceDelta distanceDelta) {
		long normXUnscaled = ARITHMETIC.divide(distanceDelta.deltaX.unscaledValue(), distanceUnscaled);
		long normYUnscaled = ARITHMETIC.divide(distanceDelta.deltaY.unscaledValue(), distanceUnscaled);

		return new NormalizedDirection(normXUnscaled, normYUnscaled);
	}

	/**
	 * Scale the normalized direction by the unscaled displacement magnitude to
	 * produce an unscaled displacement vector for this tick.
	 */
	private DisplacementVector calculateDisplacementVector(long displacementUnscaled,
			NormalizedDirection normalizedDirection) {

		long dispXUnscaled = ARITHMETIC.multiply(normalizedDirection.normXUnscaled, displacementUnscaled);
		long dispYUnscaled = ARITHMETIC.multiply(normalizedDirection.normYUnscaled, displacementUnscaled);

		return new DisplacementVector(dispXUnscaled, dispYUnscaled);
	}

	/**
	 * Convert the unscaled displacement vector into a new {@link Position}
	 * by adding it to the current position's unscaled values and wrapping back
	 * into Decimal&lt;Scale4f&gt; via {@link DecimalUtils#fromUnscaled}.
	 */
	private Position calculateNewPosition(Position position, DisplacementVector displacementVector) {
		long newX = ARITHMETIC.add(position.x().unscaledValue(), displacementVector.dispXUnscaled);
		long newY = ARITHMETIC.add(position.y().unscaledValue(), displacementVector.dispYUnscaled);

		return new Position(DecimalUtils.fromUnscaled(newX), DecimalUtils.fromUnscaled(newY));
	}
}
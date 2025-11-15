package com.cosmic.scavengers.system;

import static com.cosmic.scavengers.util.DecimalUtils.ARITHMETIC;

import org.decimal4j.api.Decimal;
import org.decimal4j.scale.Scale4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.component.Movement;
import com.cosmic.scavengers.component.Position;
import com.cosmic.scavengers.util.DecimalUtils;
import com.cosmic.scavengers.util.meta.GameDecimal;

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

	private class DistanceDelta {
		long deltaXUnscaled;
		long deltaYUnscaled;

		DistanceDelta(long deltaXUnscaled, long deltaYUnscaled) {
			this.deltaXUnscaled = deltaXUnscaled;
			this.deltaYUnscaled = deltaYUnscaled;
		}
	}

	private class NormalizedDirection {
		GameDecimal normX;
		GameDecimal normY;

		NormalizedDirection(GameDecimal normX, GameDecimal normY) {
			this.normX = normX;
			this.normY = normY;
		}
	}

	private class DisplacementVector {
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
			final Entity entity = result.entity();
			final Position position = result.comp1();
			final Movement movement = result.comp2();

			// Calculate Distance Delta = Target Position - Current Position
			DistanceDelta distanceDelta = calculateDistanceDelta(position, movement);

			// Snap to target if within threshold
			long distanceSquaredUnscaled = calculateDistanceSquaredUnscaled(distanceDelta);
			if (distanceSquaredUnscaled <= THRESHOLD_SQUARED_UNSCALED) {
				handleSnapCondition(entity, movement);
				return;
			}

			// Displacement Magnitude (DM) = Speed * Time Delta
			long displacementUnscaled = ARITHMETIC.multiply(movement.speed().unscaledValue(),
					TICK_DELTA.unscaledValue());
			// Normalized Direction Vector (NDV) = Delta / Distance
			NormalizedDirection normalizedDirection = calculateNormalizedDirection(distanceSquaredUnscaled,
					distanceDelta);

			// Displacement Vector = (NDV * DM)
			DisplacementVector displacementVector = calculateDisplacementVector(displacementUnscaled,
					normalizedDirection);

			// New Position = Current Position + Displacement Vector
			Position newPostion = calculateNewPosition(position, displacementVector);

			// Update the entity's position component
			entity.add(newPostion);
			log.debug("Entity {} moved to ({}, {})", entity.getName(), newPostion.x(), newPostion.y());

		});
	}

	private DistanceDelta calculateDistanceDelta(Position position, Movement movement) {
		// Current Position
		GameDecimal positionX = position.x();
		GameDecimal positionY = position.y();

		// Target Position
		GameDecimal movementTargetX = movement.targetX();
		GameDecimal movementTargetY = movement.targetY();

		// Delta = Target - Current
		long deltaXUnscaled = ARITHMETIC.subtract(movementTargetX.unscaledValue(), positionX.unscaledValue());
		long deltaYUnscaled = ARITHMETIC.subtract(movementTargetY.unscaledValue(), positionY.unscaledValue());

		return new DistanceDelta(deltaXUnscaled, deltaYUnscaled);
	}

	private long calculateDistanceSquaredUnscaled(DistanceDelta distanceDelta) {
		// Distance Squared = (DeltaX^2) + (DeltaY^2)
		long deltaXSquaredUnscaled = ARITHMETIC.multiply(distanceDelta.deltaXUnscaled, distanceDelta.deltaXUnscaled);
		long deltaYSquaredUnscaled = ARITHMETIC.multiply(distanceDelta.deltaYUnscaled, distanceDelta.deltaYUnscaled);

		return ARITHMETIC.add(deltaXSquaredUnscaled, deltaYSquaredUnscaled);
	}

	private void handleSnapCondition(Entity entity, Movement movement) {
		// Reached target: snap Position and remove Movement component
		Position finalPosition = new Position(movement.targetX(), movement.targetY());

		entity.add(finalPosition);
		entity.removeType(Movement.class);

		log.debug("Entity {} reached target ({}, {}). Movement stopped.", entity.getName(), movement.targetX(),
				movement.targetY());
	}

	private NormalizedDirection calculateNormalizedDirection(long distanceSquaredUnscaled,
			DistanceDelta distanceDelta) {
		long distanceUnscaled = ARITHMETIC.sqrt(distanceSquaredUnscaled);

		// Normalized Direction Vector (Delta / Distance)
		long normXUnscaled = ARITHMETIC.divide(distanceDelta.deltaXUnscaled, distanceUnscaled);
		long normYUnscaled = ARITHMETIC.divide(distanceDelta.deltaYUnscaled, distanceUnscaled);

		GameDecimal normX = DecimalUtils.fromUnscaled(normXUnscaled);
		GameDecimal normY = DecimalUtils.fromUnscaled(normYUnscaled);

		return new NormalizedDirection(normX, normY);
	}

	private DisplacementVector calculateDisplacementVector(long displacementUnscaled,
			NormalizedDirection normalizedDirection) {

		// Displacement Vector = (NDV * DM)	
		long dispXUnscaled = ARITHMETIC.multiply(normalizedDirection.normX.unscaledValue(), displacementUnscaled);
		long dispYUnscaled = ARITHMETIC.multiply(normalizedDirection.normY.unscaledValue(), displacementUnscaled);

		return new DisplacementVector(dispXUnscaled, dispYUnscaled);
	}

	private Position calculateNewPosition(Position position, DisplacementVector displacementVector) {
		long newX = ARITHMETIC.add(position.x().unscaledValue(), displacementVector.dispXUnscaled);
		long newY = ARITHMETIC.add(position.y().unscaledValue(), displacementVector.dispYUnscaled);

		return new Position(DecimalUtils.fromUnscaled(newX), DecimalUtils.fromUnscaled(newY));

	}
}

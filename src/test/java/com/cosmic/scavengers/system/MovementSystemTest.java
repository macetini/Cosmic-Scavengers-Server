package com.cosmic.scavengers.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.decimal4j.api.Decimal;
import org.decimal4j.scale.Scale4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.component.Movement;
import com.cosmic.scavengers.component.Position;
import com.cosmic.scavengers.system.MovementSystem.DisplacementVector;
import com.cosmic.scavengers.system.MovementSystem.DistanceDelta;
import com.cosmic.scavengers.system.MovementSystem.NormalizedDirection;
import com.cosmic.scavengers.util.DecimalUtils;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results;
import dev.dominion.ecs.api.Results.With2;

class MovementSystemTest {
	private static final Logger log = LoggerFactory.getLogger(MovementSystemTest.class);

	private MovementSystem movementSystem;

	@Mock
	private Dominion dominion;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		movementSystem = new MovementSystem(dominion);
	}

	@Test
	void test_CalculateDistanceDelta_PositiveVector() {
		final Decimal<Scale4f> startX = DecimalUtils.fromInteger(0L);
		final Decimal<Scale4f> startY = DecimalUtils.fromInteger(0L);

		final Position position = new Position(startX, startY);

		final Decimal<Scale4f> targetX = DecimalUtils.fromInteger(10L);
		final Decimal<Scale4f> targetY = DecimalUtils.fromInteger(5L);

		final Decimal<Scale4f> speed = DecimalUtils.fromInteger(1L);

		final Movement movement = new Movement(targetX, targetY, speed);

		final Method method;
		try {
			method = MovementSystem.class.getDeclaredMethod("calculateDistanceDelta", Position.class, Movement.class);
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}
		method.setAccessible(true);

		final MovementSystem.DistanceDelta distanceDelta;
		try {
			distanceDelta = (MovementSystem.DistanceDelta) method.invoke(movementSystem, position, movement);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}

		// Expected Delta X: Target X (100000L) - Start X (0L) = 100000L
		final Decimal<Scale4f> expectedDeltaX = DecimalUtils.fromInteger(10L);
		assertEquals(expectedDeltaX, distanceDelta.deltaX, "DeltaX should be 100000L (10.0).");

		// Expected Delta Y: Target Y (50000L) - Start Y (0L) = 50000L
		final Decimal<Scale4f> expectedDeltaY = DecimalUtils.fromInteger(5L);
		assertEquals(expectedDeltaY, distanceDelta.deltaY, "DeltaY should be 50000L (5.0).");
	}

	@Test
	void test_CalculateDistanceSquaredUnscaled_Pythagoras() {
		// DeltaX = 3.0 (Unscaled 30000L)
		final Decimal<Scale4f> deltaX = DecimalUtils.fromInteger(3L);
		// DeltaY = 4.0 (Unscaled 40000L)
		final Decimal<Scale4f> deltaY = DecimalUtils.fromInteger(4L);

		final DistanceDelta distanceDelta = new MovementSystem.DistanceDelta(deltaX, deltaY);

		final Method method;
		try {
			method = MovementSystem.class.getDeclaredMethod("calculateDistanceSquaredUnscaled", DistanceDelta.class);
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}
		method.setAccessible(true);

		final long distanceSquaredUnscaled;
		try {
			distanceSquaredUnscaled = (long) method.invoke(movementSystem, distanceDelta);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}

		// Expected Result: (3.0^2) + (4.0^2) = 9.0 + 16.0 = 25.0
		// Unscaled: 25.0 * 10000 = 250000L
		final long expectedResult = 250000L;

		assertEquals(expectedResult, distanceSquaredUnscaled,
				"Distance squared should equal 25.0 (250000L) calculated using Pythagoras.");
	}

	@Test
	void test_HandleSnapCondition_PositionAndRemoval() {
		final Entity mockEntity = mock(Entity.class);
		when(mockEntity.getName()).thenReturn("TestEntity");

		final Decimal<Scale4f> targetX = DecimalUtils.fromInteger(1L);
		final Decimal<Scale4f> targetY = DecimalUtils.fromInteger(0L);
		final Decimal<Scale4f> speed = DecimalUtils.fromInteger(5L);
		final Movement movement = new Movement(targetX, targetY, speed);

		mockEntity.add(movement);

		final Decimal<Scale4f> positionX = DecimalUtils.fromInteger(0L);
		final Decimal<Scale4f> positionY = DecimalUtils.fromInteger(0L);
		Position position = new Position(positionX, positionY);

		mockEntity.add(position);

		final Method method;
		try {
			method = MovementSystem.class.getDeclaredMethod("handleSnapCondition", Entity.class, Movement.class);
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}
		method.setAccessible(true);

		try {
			method.invoke(movementSystem, mockEntity, movement);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("Failed to invoke method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}

		verify(mockEntity).removeType(Movement.class);

		final ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
		verify(mockEntity, atLeast(1)).add(positionCaptor.capture());

		final Position finalPosition = positionCaptor.getValue();

		assertEquals(movement.targetX(), finalPosition.x(), "The movement X position must exactly match the target X.");
		assertEquals(movement.targetY(), finalPosition.y(), "The movement Y position must exactly match the target Y.");

	}

	@Test
	void test_CalculateNormalizedDirection_BasicVector() {
		// Right triangle (the 3-4-5 Pythagorean triple)
		final Decimal<Scale4f> deltaX = DecimalUtils.fromInteger(3L);
		final Decimal<Scale4f> deltaY = DecimalUtils.fromInteger(4L);

		final DistanceDelta distanceDelta = new DistanceDelta(deltaX, deltaY);

		// Distance (Magnitude) = 5.0 (Unscaled 50000L)
		final long distanceUnscaled = 50000L;

		final Method method;
		try {
			method = MovementSystem.class.getDeclaredMethod("calculateNormalizedDirection", long.class,
					DistanceDelta.class);
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}
		method.setAccessible(true);

		final NormalizedDirection normalizedDirection;
		try {
			normalizedDirection = (NormalizedDirection) method.invoke(movementSystem, distanceUnscaled, distanceDelta);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}

		// Expected NormX: 3.0 / 5.0 = 0.6 (Unscaled 6000L)
		final long expectedNormX = 6000L;
		assertEquals(expectedNormX, normalizedDirection.normXUnscaled, "Normalized X should be 0.6 (6000L).");

		// Expected NormY: 4.0 / 5.0 = 0.8 (Unscaled 8000L)
		final long expectedNormY = 8000L;
		assertEquals(expectedNormY, normalizedDirection.normYUnscaled, "Normalized Y should be 0.8 (8000L).");
	}

	@Test
	void test_CalculateDisplacementVector_BasicScale() {
		// Normalized Direction (0.6, 0.8) for a 3-4-5 triangle
		final long normXUnscaled = 6000L;
		final long normYUnscaled = 8000L;
		final NormalizedDirection normalizedDirection = new NormalizedDirection(normXUnscaled, normYUnscaled);

		// Displacement Magnitude (DM) = 2.0 (Unscaled 20000L)
		final long displacementUnscaled = 20000L;

		final Method method;
		try {
			method = MovementSystem.class.getDeclaredMethod("calculateDisplacementVector", long.class,
					NormalizedDirection.class);
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}
		method.setAccessible(true);

		// Invoke the private method
		final DisplacementVector displacementVector;
		try {
			displacementVector = (DisplacementVector) method.invoke(movementSystem, displacementUnscaled,
					normalizedDirection);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}

		// Expected DispX: 0.6 * 2.0 = 1.2 (Unscaled 12000L)
		final long expectedDispX = 12000L;
		assertEquals(expectedDispX, displacementVector.dispXUnscaled, "Displacement X should be 1.2 (12000L).");

		// Expected DispY: 0.8 * 2.0 = 1.6 (Unscaled 16000L)
		final long expectedDispY = 16000L;
		assertEquals(expectedDispY, displacementVector.dispYUnscaled, "Displacement Y should be 1.6 (16000L).");
	}

	@Test
	void test_CalculateNewPosition_Addition() {
		// Start Position: (1.0, 5.0) -> Use DecimalUtils.fromInteger if 1L maps to
		// 10000L
		final Decimal<Scale4f> startX = DecimalUtils.fromInteger(1L);
		final Decimal<Scale4f> startY = DecimalUtils.fromInteger(5L);
		final Position position = new Position(startX, startY);

		// Displacement Vector: (0.5, 0.2) -> Pass raw unscaled values (5000L and 2000L)
		final long dispXUnscaled = 5000L;
		final long dispYUnscaled = 2000L;
		final DisplacementVector displacementVector = new DisplacementVector(dispXUnscaled, dispYUnscaled);

		final Method method;
		try {
			method = MovementSystem.class.getDeclaredMethod("calculateNewPosition", Position.class,
					DisplacementVector.class);
		} catch (NoSuchMethodException | SecurityException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}
		method.setAccessible(true);

		final Position newPosition;
		try {
			newPosition = (Position) method.invoke(movementSystem, position, displacementVector);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("Failed to retrieve method via reflection.", e);
			throw new RuntimeException("Failed to invoke private method for testing.", e);
		}

		// Expected New X: 1.0 + 0.5 = 1.5 (Unscaled 15000L)
		final long expectedX = 15000L;
		assertEquals(expectedX, newPosition.x().unscaledValue(), "New X position should be 1.5 (15000L).");

		// Expected New Y: 5.0 + 0.2 = 5.2 (Unscaled 52000L)
		final long expectedY = 52000L;
		assertEquals(expectedY, newPosition.y().unscaledValue(), "New Y position should be 5.2 (52000L).");
	}

	/**
	 * Test case 1: Verifies basic movement exactly one step towards the target.
	 */

	@Test
	void test_Whole_BasicMovementTowardsTarget() {
		// Position (0.0, 0.0)
		final Decimal<Scale4f> startX = DecimalUtils.fromInteger(0L);
		final Decimal<Scale4f> startY = DecimalUtils.fromInteger(0L);
		final Position position = new Position(startX, startY);

		// Target (10.0, 0.0) with Speed (1.0)
		final Decimal<Scale4f> targetX = DecimalUtils.fromInteger(10L);
		final Decimal<Scale4f> targetY = DecimalUtils.fromInteger(0L);
		final Decimal<Scale4f> speed = DecimalUtils.fromInteger(1L);
		final Movement movement = new Movement(targetX, targetY, speed);

		final Entity mockEntity = mock(Entity.class);
		when(mockEntity.getName()).thenReturn("TestEntity");

		final With2<Position, Movement> mockWith2 = new With2<>(position, movement, mockEntity);
		final Stream<With2<Position, Movement>> mockStream = Stream.of(mockWith2);

		@SuppressWarnings("unchecked")
		final Results<With2<Position, Movement>> mockResults = mock(Results.class);

		when(mockResults.stream()).thenReturn(mockStream);

		when(dominion.findEntitiesWith(Position.class, Movement.class)).thenReturn((Results) mockResults);

		// --- Execute System ---
		movementSystem.run();

		// --- Verify Final State ---
		final ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
		verify(mockEntity).add(positionCaptor.capture());

		final Position finalPosition = positionCaptor.getValue();

		// Expected Movement: 0.1 units (1000L unscaled)
		final long expectedXUnscaled = 1000L; // 0.1 units

		// Expected X position: 0 + 0.1 = 0.1 (Unscaled: 1000L)
		assertEquals(expectedXUnscaled, finalPosition.x().unscaledValue(),
				"X position should have moved 0.1 units (1000L).");
		assertEquals(startY.unscaledValue(), finalPosition.y().unscaledValue(), "Y position should not have changed.");

		// The entity should NOT snap, as it's only 0.1 units toward a 10.0 target.
		verify(mockEntity, never()).removeType(Movement.class);
	}
}
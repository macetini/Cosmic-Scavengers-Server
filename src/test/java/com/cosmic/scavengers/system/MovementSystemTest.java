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

import com.cosmic.scavengers.core.utils.DecimalUtils;
import com.cosmic.scavengers.ecs.domain.components.Movement;
import com.cosmic.scavengers.ecs.domain.components.Position;
import com.cosmic.scavengers.system.MovementSystem.DisplacementVector;
import com.cosmic.scavengers.system.MovementSystem.DistanceDelta;
import com.cosmic.scavengers.system.MovementSystem.NormalizedDirection;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results;
import dev.dominion.ecs.api.Results.With2;

/**
 * Unit tests for {@link MovementSystem}.
 *
 * <p>
 * This test class exercises internal movement math via reflection and the
 * public run() behavior via a mocked Dominion/Results stream. Tests are written
 * against fixed-point Decimal values (Decimal4j) and assert on the unscaled
 * values which represent the deterministic fixed-point units used throughout
 * the movement system.
 *
 * <p>
 * Key goals covered by tests:
 * <ul>
 * <li>Verify low-level helpers (distance delta, squared distance,
 * normalization, displacement scaling, and position addition) behave as
 * expected for common numeric cases (e.g., 3-4-5 triangle).</li>
 * <li>Verify the snap behavior which places an entity exactly at its target and
 * removes the Movement component.</li>
 * <li>Verify that {@link MovementSystem#run()} advances an entity by a single
 * tick when appropriate (speed * tick delta) using a mocked Dominion result
 * set.</li>
 * </ul>
 */
class MovementSystemTest {
	private static final Logger log = LoggerFactory.getLogger(MovementSystemTest.class);

	private MovementSystem movementSystem;

	@Mock
	private Dominion dominion;

	/**
	 * Create a fresh MovementSystem using a mocked Dominion before each test.
	 */
	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		movementSystem = new MovementSystem(dominion);
	}

	// AI Generated Tests (Gemini)

	/**
	 * Tests calculateDistanceDelta for a 3D vector.
	 *
	 * Scenario: - Start (0,0,0), Target (10,5,2), Speed 1.0
	 *
	 * Expectations: - DeltaX equals 10.0, DeltaY equals 5.0, and DeltaZ equals 2.0
	 * (validated via Decimal equality)
	 */
	@Test
	void test_CalculateDistanceDelta_3D() {
		final Position position = new Position(
				DecimalUtils.fromScaled(0L), 
				DecimalUtils.fromScaled(0L),
				DecimalUtils.fromScaled(0L));

		final Movement movement = new Movement(
				DecimalUtils.fromScaled(10L),	// Target X
				DecimalUtils.fromScaled(5L),	// Target Y
				DecimalUtils.fromScaled(2L), 	// Target Z
				DecimalUtils.fromScaled(1L));	// Speed

		final Method method = getPrivateMethod("calculateDistanceDelta", Position.class, Movement.class);
		final DistanceDelta delta = (DistanceDelta) invokeMethod(method, position, movement);

		assertEquals(100000L, delta.deltaX.unscaledValue());
		assertEquals(50000L, delta.deltaY.unscaledValue());
		assertEquals(20000L, delta.deltaZ.unscaledValue());
	}

	/**
	 * Tests 3D Pythagoras: 3^2 + 4^2 + 12^2 = 9 + 16 + 144 = 169. Distance should
	 * be 13.
	 */
	@Test
	void test_CalculateDistanceSquaredUnscaled_3DPythagoras() {
		final DistanceDelta delta = new DistanceDelta(
				DecimalUtils.fromScaled(3L), 
				DecimalUtils.fromScaled(4L),
				DecimalUtils.fromScaled(12L));

		final Method method = getPrivateMethod("calculateDistanceSquaredUnscaled", DistanceDelta.class);
		final long distanceSquaredUnscaled = (long) invokeMethod(method, delta);

		// (3.0^2 + 4.0^2 + 12.0^2) in Scale4f:
		// 30000^2 + 40000^2 + 120000^2 = 900M + 1600M + 14400M = 16900M
		// But wait: DecimalArithmetic.multiply(a, a) returns (a*a)/10000.
		// So: 90000 + 160000 + 1440000 = 1,690,000L.
		final long expectedResult = 1690000L;

		assertEquals(expectedResult, distanceSquaredUnscaled,
				"3D Distance squared should be deterministic in Scale4f.");
	}

	@Test
	void test_CalculateNormalizedDirection_3D_Accuracy() {
		final DistanceDelta delta = new DistanceDelta(
				DecimalUtils.fromScaled(3L), 
				DecimalUtils.fromScaled(4L),
				DecimalUtils.fromScaled(12L));

		// Total distance is sqrt(1.69) = 1.3 in decimals?
		// No, sqrt(1690000) = 1300. (1300 units is 0.13)
		// Let's use coordinates that result in exactly 13.0 distance.
		// Delta 3.0, 4.0, 12.0 -> DistSquared = 169.0 -> Distance = 13.0
		// distanceUnscaled = 130000L
		final long distanceUnscaled = 130000L;

		final Method method = getPrivateMethod("calculateNormalizedDirection", long.class, DistanceDelta.class);
		final NormalizedDirection norm = (NormalizedDirection) invokeMethod(method, distanceUnscaled, delta);

		// Expectations: (Components / 13.0) * 10000
		// 3/13 ≈ 0.230769... -> 2308
		// 4/13 ≈ 0.307692... -> 3077
		// 12/13 ≈ 0.923076... -> 9231
		assertEquals(2308L, norm.normXUnscaled, "NormX should be ~0.2308");
		assertEquals(3077L, norm.normYUnscaled, "NormY should be ~0.3077");
		assertEquals(9231L, norm.normZUnscaled, "NormZ should be ~0.9231");
	}

	/**
	 * Tests movement for a single tick.
	 */
	@Test
	void test_Whole_3DMovement_SingleTick() {
		final Position start = new Position(
				DecimalUtils.fromScaled(0L), 
				DecimalUtils.fromScaled(0L),
				DecimalUtils.fromScaled(0L));
		
		// Target 10 units away on X. Speed 1 unit/sec.
		final Movement move = new Movement(
				DecimalUtils.fromScaled(10L), 
				DecimalUtils.fromScaled(0L),
				DecimalUtils.fromScaled(0L), 
				DecimalUtils.fromScaled(1L));

		final Entity mockEntity = mock(Entity.class);
		when(mockEntity.getName()).thenReturn("TestEntity3D");

		final With2<Position, Movement> mockWith2 = new With2<>(start, move, mockEntity);
		@SuppressWarnings("unchecked")
		final Results<With2<Position, Movement>> mockResults = mock(Results.class);
		when(mockResults.stream()).thenReturn(Stream.of(mockWith2));
		when(dominion.findEntitiesWith(Position.class, Movement.class)).thenReturn((Results) mockResults);

		movementSystem.run();

		final ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
		verify(mockEntity).add(positionCaptor.capture());

		// Tick is 0.1s, Speed is 1.0 -> Move 0.1 units (1000L unscaled)
		assertEquals(1000L, positionCaptor.getValue().x().unscaledValue());
		assertEquals(0L, positionCaptor.getValue().y().unscaledValue());
		assertEquals(0L, positionCaptor.getValue().z().unscaledValue());
	}

	// My Tests

	/**
	 * Tests calculateDistanceDelta for a simple positive vector.
	 *
	 * Scenario: - Start (0,0, 0), Target (10,5, 2), Speed 1.0
	 *
	 * Expectations: - DeltaX equals 10.0, DeltaY equals 5.0 and DeltaZ equals 2.0
	 * (validated via Decimal equality)
	 */
	@Test
	void test_CalculateDistanceDelta_PositiveVector() {
		// Arrange: Start (0,0,0) -> Target (10, 5, 2)
		final Decimal<Scale4f> startX = DecimalUtils.fromScaled(0L);
		final Decimal<Scale4f> startY = DecimalUtils.fromScaled(0L);
		final Decimal<Scale4f> startZ = DecimalUtils.fromScaled(0L);

		final Position position = new Position(startX, startY, startZ);

		final Decimal<Scale4f> targetX = DecimalUtils.fromScaled(10L);
		final Decimal<Scale4f> targetY = DecimalUtils.fromScaled(5L);
		final Decimal<Scale4f> targetZ = DecimalUtils.fromScaled(2L);

		final Decimal<Scale4f> speed = DecimalUtils.fromScaled(1L);

		final Movement movement = new Movement(targetX, targetY, targetZ, speed);

		final Method method = getPrivateMethod("calculateDistanceDelta", Position.class, Movement.class);
		final MovementSystem.DistanceDelta distanceDelta = (MovementSystem.DistanceDelta) invokeMethod(method, position,
				movement);

		// We'll use unscaled values for all three to ensure strict deterministic
		// equality
		assertEquals(100000L, distanceDelta.deltaX.unscaledValue(), "DeltaX should be 10.0 (100000L).");
		assertEquals(50000L, distanceDelta.deltaY.unscaledValue(), "DeltaY should be 5.0 (50000L).");
		assertEquals(20000L, distanceDelta.deltaZ.unscaledValue(), "DeltaZ should be 2.0 (20000L).");
	}

	/**
	 * Tests calculateDistanceSquaredUnscaled implements the 3D Pythagorean theorem.
	 *
	 * Scenario: deltaX=3.0, deltaY=4.0, deltaZ=12.0 Math: 3.0^2 + 4.0^2 + 12.0^2 =
	 * 9 + 16 + 144 = 169.0
	 */
	@Test
	void test_CalculateDistanceSquaredUnscaled_Pythagoras_3D() {
		// Arrange: Use a 3-4-12 vector
		final DistanceDelta distanceDelta = new MovementSystem.DistanceDelta(
				DecimalUtils.fromScaled(3L),
				DecimalUtils.fromScaled(4L), 
				DecimalUtils.fromScaled(12L));

		// Act: Use your helper methods
		final Method method = getPrivateMethod("calculateDistanceSquaredUnscaled", DistanceDelta.class);
		final long distanceSquaredUnscaled = (long) invokeMethod(method, distanceDelta);

		// Assert:
		// In Scale4f, 169.0 is represented as 1,690,000L.
		// (Each square component is (value^2)/10000)
		final long expectedResult = 1690000L;

		assertEquals(expectedResult, distanceSquaredUnscaled,
				"Distance squared should equal 169.0 (1690000L) for a 3-4-12 vector.");
	}

	/**
	 * Tests that handleSnapCondition places the entity exactly at the target
	 * position (3D) and removes the Movement component from the entity.
	 */
	@Test
	void test_HandleSnapCondition_PositionAndRemoval() {
		// Arrange
		final Entity mockEntity = mock(Entity.class);
		when(mockEntity.getName()).thenReturn("TestEntity");

		// Target: (3, 4, 12) | Speed: 14.0
		final Movement movement = new Movement(
				DecimalUtils.fromScaled(3L), 
				DecimalUtils.fromScaled(4L),
				DecimalUtils.fromScaled(12L), 
				DecimalUtils.fromScaled(14L));

		// Act: Use your helpers to invoke the private snap logic
		final Method method = getPrivateMethod("handleSnapCondition", Entity.class, Movement.class);
		invokeMethod(method, mockEntity, movement);

		// Assert: 1. Movement component must be removed
		verify(mockEntity).removeType(Movement.class);

		// Assert: 2. Position must be updated to match the target exactly
		final ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
		verify(mockEntity, atLeast(1)).add(positionCaptor.capture());

		final Position finalPosition = positionCaptor.getValue();

		assertEquals(movement.targetX(), finalPosition.x(), "X must match target X.");
		assertEquals(movement.targetY(), finalPosition.y(), "Y must match target Y.");
		assertEquals(movement.targetZ(), finalPosition.z(), "Z must match target Z.");
	}

	/**
	 * Tests calculateNormalizedDirection for a 3-4-12 vector ensuring the
	 * normalized components are correct in unscaled units.
	 */
	@Test
	void test_CalculateNormalizedDirection_3D() {
		// Arrange: 3-4-12 triangle
		final Decimal<Scale4f> deltaX = DecimalUtils.fromScaled(3L);
		final Decimal<Scale4f> deltaY = DecimalUtils.fromScaled(4L);
		final Decimal<Scale4f> deltaZ = DecimalUtils.fromScaled(12L);

		final DistanceDelta distanceDelta = new DistanceDelta(deltaX, deltaY, deltaZ);

		// Total distance is sqrt(3^2 + 4^2 + 12^2) = 13.0
		// Unscaled: 13.0 * 10000 = 130000L
		final long distanceUnscaled = 130000L;

		// Act: Use your helpers
		final Method method = getPrivateMethod("calculateNormalizedDirection", long.class, DistanceDelta.class);
		final NormalizedDirection norm = (NormalizedDirection) invokeMethod(method, distanceUnscaled, distanceDelta);

		// Assert: Components / 13.0
		// Expected NormX: 3 / 13 ≈ 0.230769... -> 2308 (at Scale4f)
		// Expected NormY: 4 / 13 ≈ 0.307692... -> 3077
		// Expected NormZ: 12 / 13 ≈ 0.923076... -> 9231
		assertEquals(2308L, norm.normXUnscaled, "Normalized X should be ~0.2308 (2308L).");
		assertEquals(3077L, norm.normYUnscaled, "Normalized Y should be ~0.3077 (3077L).");
		assertEquals(9231L, norm.normZUnscaled, "Normalized Z should be ~0.9231 (9231L).");
	}

	/**
	 * Tests calculateDisplacementVector scales a normalized direction (3-4-5) by a
	 * displacement magnitude, producing exact unscaled displacement.
	 */
	@Test
	void test_CalculateDisplacementVector_BasicScale() {
		// Arrange: Normalized Direction (0.6, 0.8, 0.0)
		// Scale4f: 0.6 * 10000 = 6000L
		final long normXUnscaled = 6000L;
		final long normYUnscaled = 8000L;
		final long normZUnscaled = 0L;
		final NormalizedDirection normalizedDirection = new NormalizedDirection(normXUnscaled, normYUnscaled,
				normZUnscaled);

		// Displacement Magnitude (DM) = 5.0 (Unscaled 50000L)
		final long displacementUnscaled = 50000L;

		// Act: Use your existing helper methods
		final Method method = getPrivateMethod("calculateDisplacementVector", long.class, NormalizedDirection.class);
		final DisplacementVector displacementVector = (DisplacementVector) invokeMethod(method, displacementUnscaled,
				normalizedDirection);

		// Assert:
		// Expected DispX: 0.6 * 5.0 = 3.0 (Unscaled 30000L)
		// Expected DispY: 0.8 * 5.0 = 4.0 (Unscaled 40000L)
		assertEquals(30000L, displacementVector.dispXUnscaled, "Displacement X should be exactly 3.0 (30000L).");
		assertEquals(40000L, displacementVector.dispYUnscaled, "Displacement Y should be exactly 4.0 (40000L).");
		assertEquals(0L, displacementVector.dispZUnscaled, "Displacement Z should be exactly 0.0 (0L).");
	}

	/**
	 * Tests calculateNewPosition correctly adds an unscaled displacement to the
	 * current position in 3D and returns a new {@link Position}.
	 */
	@Test
	void test_CalculateNewPosition_Addition() {
		// Arrange: Start Position (1.0, 5.0, 10.0)
		final Position position = new Position(
				DecimalUtils.fromScaled(1L), 
				DecimalUtils.fromScaled(5L),
				DecimalUtils.fromScaled(10L));

		// Displacement Vector: (0.5, 0.2, -0.3) -> Raw unscaled: 5000L, 2000L, -3000L
		final long dispXUnscaled = 5000L;
		final long dispYUnscaled = 2000L;
		final long dispZUnscaled = -3000L;
		final DisplacementVector displacementVector = new DisplacementVector(dispXUnscaled, dispYUnscaled,
				dispZUnscaled);

		// Act: Use your existing helper methods
		final Method method = getPrivateMethod("calculateNewPosition", Position.class, DisplacementVector.class);
		final Position newPosition = (Position) invokeMethod(method, position, displacementVector);

		// Assert
		// Expected X: 1.0 + 0.5 = 1.5 (15000L)
		assertEquals(15000L, newPosition.x().unscaledValue(), "New X position should be 1.5 (15000L).");

		// Expected Y: 5.0 + 0.2 = 5.2 (52000L)
		assertEquals(52000L, newPosition.y().unscaledValue(), "New Y position should be 5.2 (52000L).");

		// Expected Z: 10.0 - 0.3 = 9.7 (97000L)
		assertEquals(97000L, newPosition.z().unscaledValue(), "New Z position should be 9.7 (97000L).");
	}

	/**
	 * Verifies that a single run() call advances an entity one tick toward a 3D
	 * target.
	 *
	 * Scenario: - Start (0,0,0), Target (10,10,10), Speed 1.0 - Distance ≈ 17.32.
	 * Tick = 0.1s. - Displacement should be speed * tick = 0.1 total.
	 */
	@Test
	@SuppressWarnings("unchecked")
	void test_Whole_3DMovementTowardsTarget() {
		// Arrange: Start at origin
		final Position startPos = new Position(
				DecimalUtils.fromScaled(0L), 
				DecimalUtils.fromScaled(0L),
				DecimalUtils.fromScaled(0L));

		// Target (10,0,0) - Keeping it simple on X-axis for clear assertion math
		final Movement move = new Movement(
				DecimalUtils.fromScaled(10L), 
				DecimalUtils.fromScaled(0L),
				DecimalUtils.fromScaled(0L), 
				DecimalUtils.fromScaled(1L));

		final Entity mockEntity = mock(Entity.class);
		when(mockEntity.getName()).thenReturn("SpaceExplorer");

		// Dominion Mocking
		final With2<Position, Movement> mockWith2 = new With2<>(startPos, move, mockEntity);
		final Results<With2<Position, Movement>> mockResults = mock(Results.class);
		when(mockResults.stream()).thenReturn(Stream.of(mockWith2));
		when(dominion.findEntitiesWith(Position.class, Movement.class)).thenReturn((Results) mockResults);

		// Act
		movementSystem.run();

		// Assert
		final ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
		verify(mockEntity).add(positionCaptor.capture());

		final Position finalPosition = positionCaptor.getValue();

		// With speed 1.0 and tick 0.1s, movement is exactly 0.1 units (1000L)
		assertEquals(1000L, finalPosition.x().unscaledValue(), "X should advance by 0.1 units.");
		assertEquals(0L, finalPosition.y().unscaledValue(), "Y should remain 0.");
		assertEquals(0L, finalPosition.z().unscaledValue(), "Z should remain 0.");

		// Ensure we haven't reached target yet, so Movement remains
		verify(mockEntity, never()).removeType(Movement.class);
	}

	// --- HELPER METHODS ---
	private Method getPrivateMethod(String name, Class<?>... parameterTypes) {
		try {
			Method method = MovementSystem.class.getDeclaredMethod(name, parameterTypes);
			method.setAccessible(true);
			return method;
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private Object invokeMethod(Method method, Object... args) {
		try {
			return method.invoke(movementSystem, args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

}
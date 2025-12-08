package com.cosmic.scavengers.core;

import org.junit.jupiter.api.Test;

import com.cosmic.scavengers.util.SecurityUtils;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

	private static final String PASSWORD = "MySecurePassword123";
	private static final String WRONG_PASSWORD = "NotMySecurePassword";

	/**
	 * Test 1: Verify that the salt is generated correctly and is unique.
	 */
	@Test
	void testGenerateSalt_ShouldBeUniqueAndValid() {
		String salt1 = SecurityUtils.generateSalt();
		String salt2 = SecurityUtils.generateSalt();

		// Assert 1: Must be non-empty
		assertNotNull(salt1, "Salt must not be null.");
		assertFalse(salt1.isEmpty(), "Salt must not be empty.");

		// Assert 2: Must be unique for each call
		assertNotEquals(salt1, salt2, "Subsequent salts must be different.");

		// Assert 3: Length check (Base64 encoding of 16 bytes is 24 characters)
		assertEquals(24, salt1.length(), "Salt length should be 24 characters (Base64 encoded 16 bytes).");
	}

	/**
	 * Test 2: Verify that hashing is consistent (same password + same salt = same
	 * hash).
	 */
	@Test
	void testHashPassword_Consistency() {
		// Use a static salt to ensure repeatability
		String staticSalt = "AAAAAAAAAAAAAAAAAAAAAAAA"; // Base64 of 16 zero bytes

		String hash1 = SecurityUtils.hashPassword(PASSWORD, staticSalt);
		String hash2 = SecurityUtils.hashPassword(PASSWORD, staticSalt);

		// Assert 1: The resulting hash must be 64 characters long (SHA-256 in
		// hexadecimal)
		assertEquals(64, hash1.length(), "SHA-256 hash must be 64 hexadecimal characters.");

		// Assert 2: Consistent inputs must produce consistent outputs
		assertEquals(hash1, hash2, "Hashing the same password and salt must produce the same hash.");
	}

	/**
	 * Test 3: Verify that using a different salt with the same password produces a
	 * different hash.
	 */
	@Test
	void testHashPassword_DifferentSalt() {
		String password = PASSWORD;

		String saltA = SecurityUtils.generateSalt();
		String saltB = SecurityUtils.generateSalt();

		String hashA = SecurityUtils.hashPassword(password, saltA);
		String hashB = SecurityUtils.hashPassword(password, saltB);

		// Assert: Different salts must produce different hashes, even for the same
		// password
		assertNotEquals(hashA, hashB, "Different salts must result in different hashes.");
	}

	/**
	 * Test 4: Verify the password verification process with correct credentials.
	 */
	@Test
	void testVerifyPassword_CorrectPassword() {
		String salt = SecurityUtils.generateSalt();
		String storedHash = SecurityUtils.hashPassword(PASSWORD, salt);

		// Act & Assert: Verification with the correct password should pass
		assertTrue(SecurityUtils.verifyPassword(PASSWORD, storedHash, salt),
				"Verification should succeed with the correct password.");
	}

	/**
	 * Test 5: Verify the password verification process with incorrect credentials.
	 */
	@Test
	void testVerifyPassword_WrongPassword() {
		String salt = SecurityUtils.generateSalt();
		String storedHash = SecurityUtils.hashPassword(PASSWORD, salt);

		// Act & Assert: Verification with the wrong password should fail
		assertFalse(SecurityUtils.verifyPassword(WRONG_PASSWORD, storedHash, salt),
				"Verification should fail with the wrong password.");
	}
}
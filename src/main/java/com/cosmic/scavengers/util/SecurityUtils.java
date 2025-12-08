package com.cosmic.scavengers.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {

	private SecurityUtils() {
	}

	/**
	 * Generates a random salt (16 bytes) and encodes it in Base64. 16 bytes is
	 * equivalent to 32 hexadecimal characters or 24 Base64 characters.
	 */
	public static String generateSalt() {
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[16];
		random.nextBytes(salt);
		// Base64 is often cleaner for storing binary data in String format
		return Base64.getEncoder().encodeToString(salt);
	}

	/**
	 * Hashes a password using a provided salt with the SHA-256 algorithm. * @param
	 * password The plaintext password provided by the user.
	 * 
	 * @param salt The Base64 encoded salt string.
	 * @return The 64-character hexadecimal SHA-256 hash.
	 */
	public static String hashPassword(String password, String salt) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");

			// 1. Combine salt bytes with the password bytes for hashing
			md.update(Base64.getDecoder().decode(salt.trim()));
			byte[] hashedPassword = md.digest(password.getBytes());

			// 2. Convert the resulting hash bytes to a hexadecimal string (64 characters)
			StringBuilder sb = new StringBuilder();
			for (byte b : hashedPassword) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			// This should never happen in a standard JVM
			throw new RuntimeException("FATAL: SHA-256 algorithm not found", e);
		}
	}

	/**
	 * Verifies a provided password against the stored hash and salt from the
	 * database. * @param password The plaintext password attempt.
	 * 
	 * @param storedHash The hash stored in the database.
	 * @param storedSalt The salt stored in the database.
	 * @return True if the calculated hash matches the stored hash, false otherwise.
	 */
	public static boolean verifyPassword(String password, String storedHash, String storedSalt) {
		String calculatedHash = hashPassword(password, storedSalt);
		// Uses equals() for a safe, constant-time string comparison (important for
		// security)
		return calculatedHash.equals(storedHash);
	}
}
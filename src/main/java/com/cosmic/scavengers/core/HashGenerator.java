package com.cosmic.scavengers.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cosmic.scavengers.util.SecurityUtils;

public class HashGenerator {
	private static final Logger log = LoggerFactory.getLogger(HashGenerator.class);

	public static void main(String[] args) {
		String username = "player_1";
		String password = "secret"; // <-- Use your desired test password

		// 1. Generate secure values
		String salt = SecurityUtils.generateSalt();
		String hash = SecurityUtils.hashPassword(password, salt);

		log.info("--- Generated Credentials for Manual DB Insert ---");
		log.info("Username: {}", username);
		log.info("Password (Plaintext): {}", password);
		log.info("-------------------------------------------------");
		log.info("Salt: {}", salt);
		log.info("Hash: {}", hash);
		log.info("-------------------------------------------------");

		// 2. Ready-to-use SQL format (copy and paste the line below into DBeaver):
		log.info("SQL INSERT VALUES (Copy this entire line):");
		log.info("INSERT INTO players (username, password_hash, salt) VALUES ('{}', '{}', '{}');", username, hash,
				salt);
	}
}
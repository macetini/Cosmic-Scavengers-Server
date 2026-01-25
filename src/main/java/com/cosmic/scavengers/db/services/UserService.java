package com.cosmic.scavengers.db.services;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosmic.scavengers.core.utils.SecurityUtils;
import com.cosmic.scavengers.db.jooq.repositories.PlayerRepository;
import com.cosmic.scavengers.db.model.tables.pojos.Players;

/**
 * Service layer for player account management (Login and Registration). This
 * class orchestrates security (hashing) and data access (JPA Repository).
 */
@Service
public class UserService {
	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final PlayerRepository jooqPlayerRepository;

	public UserService(PlayerRepository playerRepository) {
		this.jooqPlayerRepository = playerRepository;
	}

	/**
	 * Registers a new user with the given username and plaintext password.
	 *
	 * @param username          The desired username for the new user.
	 * @param plaintextPassword The plaintext password for the new user.
	 * 
	 * @return An Optional containing the newly created Players object if
	 *         registration is successful, or an empty Optional if the username
	 *         already exists.
	 */
	@Transactional
	public Optional<Players> registerUser(String username, String plaintextPassword) {
		// Generate Salt and Hash using SecurityUtils
		final String salt = SecurityUtils.generateSalt();
		final String hash = SecurityUtils.hashPassword(plaintextPassword, salt);

		return registerNewPlayer(username, hash, salt);
	}

	/**
	 * Registers a new player in the database.
	 *
	 * @param username       The desired username for the new player.
	 * @param hashedPassword The hashed password for the new player.
	 * @param salt           The salt used for hashing the password.
	 * 
	 * @return An Optional containing the newly created Players object if
	 *         registration is successful, or an empty Optional if the username
	 *         already exists.
	 */
	protected Optional<Players> registerNewPlayer(String username, String hashedPassword, String salt) {
		Optional<Players> existingPlayer = jooqPlayerRepository.findByUsername(username);
		if (existingPlayer.isPresent()) {
			log.warn("Attempted to register a new player with an existing username: {}", username);
			return Optional.empty();
		}

		Players newPlayer = new Players();
		newPlayer.setUsername(username);
		newPlayer.setPasswordHash(hashedPassword);
		newPlayer.setSalt(salt);

		// Set the creation timestamp to the current UTC time
		Instant nowUtc = Instant.now();
		OffsetDateTime utcTime = OffsetDateTime.ofInstant(nowUtc, ZoneOffset.UTC);
		newPlayer.setCreatedAt(utcTime);
		newPlayer.setCurrentWorldId(1L); // Default starting world ID (will be changed to another table later)

		Players insertedPlayer = jooqPlayerRepository.insert(newPlayer);
		return Optional.of(insertedPlayer);
	}

	@Transactional
	public Optional<Players> loginUser(String username, String plaintextPassword) {
		final Optional<Players> playerOptional = jooqPlayerRepository.findByUsername(username);
		if (playerOptional.isEmpty()) {
			log.info("Login failed: User '{}' not found.", username);
			return Optional.empty(); // User not found
		}
		Players player = playerOptional.get();

		final boolean authenticated = SecurityUtils.verifyPassword(plaintextPassword, player.getPasswordHash(),
				player.getSalt());

		if (!authenticated) {
			log.info("Authentication failed for user '{}': Incorrect password.", username);
			return Optional.empty(); // Authentication failed
		}

		log.info("User '{}' logged and authenticated successfully.", username);
		return Optional.of(player);
	}
}
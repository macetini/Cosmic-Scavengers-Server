package com.cosmic.scavengers.core;

import com.cosmic.scavengers.db.UserRepository;
import com.cosmic.scavengers.engine.GameEngine;
import com.cosmic.scavengers.networking.GameChannelHandler;
import com.cosmic.scavengers.networking.NettyServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import java.util.Set;
import java.lang.reflect.Field; // Needed for reflection

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NettyServerInitializerTest {

	// Dependencies to be injected
	@Mock
	private GameEngine mockEngine;

	@Mock
	private UserRepository mockUserRepository;

	// The class under test, with mocks injected
	@InjectMocks
	private NettyServerInitializer initializer;

	// Mocks for connection management tests
	@Mock
	private GameChannelHandler mockHandler1;

	@Mock
	private GameChannelHandler mockHandler2;

	@Mock
	private GameChannelHandler mockSender;

	@BeforeEach
	void setUp() {
		// Initialize Mocks and inject them into the initializer
		MockitoAnnotations.openMocks(this);
	}

	// --- Utility to access the private Set<GameChannelHandler> channelHandlers ---
	// Since channelHandlers is private and has no getter, we use reflection in the
	// test.
	@SuppressWarnings("unchecked")
	private Set<GameChannelHandler> getChannelHandlers() throws NoSuchFieldException, IllegalAccessException {
		Field field = NettyServerInitializer.class.getDeclaredField("channelHandlers");
		field.setAccessible(true);
		return (Set<GameChannelHandler>) field.get(initializer);
	}

	// --- Test for run() method (Lifecycle) ---

	@Test
	void testRunStartsEngineAndNettyServer() throws Exception {
		// We must use MockedConstruction to intercept 'new Thread()' and 'new
		// NettyServer()'.

		// Combined try-with-resources syntax
		try (MockedConstruction<Thread> mockedThread = mockConstruction(Thread.class, (mock, context) -> {
			when(mock.getName()).thenReturn("Mocked-Thread");
		}); MockedConstruction<NettyServer> mockedServer = mockConstruction(NettyServer.class, (mock, context) -> {
			// Verify that the dependencies are passed correctly to NettyServer's
			// constructor
			assertEquals(mockEngine, context.arguments().get(0), "GameEngine was not passed correctly.");
			assertEquals(mockUserRepository, context.arguments().get(1), "UserRepository was not passed correctly.");
			assertEquals(initializer, context.arguments().get(2), "Initializer (this) was not passed correctly.");
		})) {
			// Act
			initializer.run();

			// Assert 1: Verify a Thread was constructed and started (for the GameEngine)
			verify(mockedThread.constructed().get(0), times(1)).start();

			// Assert 2: Verify NettyServer was constructed and its run() method was called
			NettyServer serverInstance = mockedServer.constructed().get(0);
			verify(serverInstance, times(1)).run();
		}
	}

	// --- Test for Connection Management ---

	@Test
	void testAddAndRemoveChannelHandler() throws NoSuchFieldException, IllegalAccessException {
		Set<GameChannelHandler> handlers = getChannelHandlers();

		// Initial state check
		assertFalse(handlers.contains(mockHandler1), "Handler should not start in set.");

		// Act 1: Add handler
		initializer.addChannelHandler(mockHandler1);

		// Assert 1: Check if the handler is successfully added
		assertTrue(handlers.contains(mockHandler1), "Handler should be added to the set.");

		// Act 2: Remove handler
		initializer.removeChannelHandler(mockHandler1);

		// Assert 2: Check if the handler is successfully removed
		assertFalse(handlers.contains(mockHandler1), "Handler should be removed from the set.");
	}

	// --- Test for IMessageBroadcaster ---

	@Test
	void testBroadcast_SendsToOthersButNotSender() {
		// Arrange: Add handlers directly using the public methods for setup
		initializer.addChannelHandler(mockHandler1);
		initializer.addChannelHandler(mockHandler2);
		initializer.addChannelHandler(mockSender);

		String testMessage = "U_POS|1|100.5|200.5";

		// Act
		initializer.broadcast(testMessage, mockSender);

		// Assert 1: Verify that the message was sent to the non-sender handlers
		verify(mockHandler1, times(1)).sendTextMessage(testMessage);
		verify(mockHandler2, times(1)).sendTextMessage(testMessage);

		// Assert 2: Verify that the message was NOT sent back to the sender
		verify(mockSender, never()).sendTextMessage(anyString());
	}

	@Test
	void testBroadcast_SendsToAllWhenNullSender() {
		// Arrange: Add handlers
		initializer.addChannelHandler(mockHandler1);
		initializer.addChannelHandler(mockHandler2);

		String testMessage = "G_INFO|GAME_STARTED";

		// Act: Use null as sender to simulate a broadcast originating from the
		// GameEngine (server-side)
		initializer.broadcast(testMessage, null);

		// Assert: Verify that the message was sent to ALL handlers
		verify(mockHandler1, times(1)).sendTextMessage(testMessage);
		verify(mockHandler2, times(1)).sendTextMessage(testMessage);
	}
}
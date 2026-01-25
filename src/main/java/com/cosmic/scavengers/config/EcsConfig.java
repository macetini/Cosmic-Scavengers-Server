package com.cosmic.scavengers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cosmic.scavengers.broadcast.IMessageBroadcaster;
import com.cosmic.scavengers.broadcast.MessageBroadcasterImpl;
import com.cosmic.scavengers.system.MovementSystem;

import dev.dominion.ecs.api.Dominion;

@Configuration
public class EcsConfig {
	@Bean
	Dominion dominion() {
		return Dominion.create();
	}

	@Bean
	MovementSystem movementSystem(Dominion dominion) {
		return new MovementSystem(dominion);
	}

	@Bean
	IMessageBroadcaster messageBroadcaster() {
		return new MessageBroadcasterImpl();
	}
}
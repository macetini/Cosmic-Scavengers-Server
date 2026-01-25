package com.cosmic.scavengers.ecs.domain.components;

/**
 * Component for the entity's current health and maximum health capacity.
 *
 * Health is represented by standard integers (int) for simplicity, as defined
 * in the Protobuf message.
 */
public record Health(int current, int max) {
}
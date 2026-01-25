package com.cosmic.scavengers.ecs.domain.tags;

/**
 * A tag component indicating the entity cannot move (e.g., Buildings,
 * Stations). Systems can use this to filter out non-movable objects.
 */
public record StaticTag() {
}

package com.cosmic.scavengers.db.jpa.model;

import java.util.List;

/**
 * A "Hot" immutable template for creating entities.
 * Uses Java 17 Record for performance and conciseness.
 */
public record BlueprintTemplate(
    String id,
    String categoryId,
    int baseHealth,
    boolean isStatic,
    List<String> traitIds,
    List<String> initialBuffIds
) {
    // Compact constructor for defensive copies
    public BlueprintTemplate {
        traitIds = List.copyOf(traitIds != null ? traitIds : List.of());
        initialBuffIds = List.copyOf(initialBuffIds != null ? initialBuffIds : List.of());
    }
}
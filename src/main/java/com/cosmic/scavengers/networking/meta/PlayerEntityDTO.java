package com.cosmic.scavengers.networking.meta;

import java.time.Instant;

import com.cosmic.scavengers.db.meta.PlayerEntity;

/**
 * Data Transfer Object (DTO) for the PlayerEntity, implemented as an immutable record.
 * This DTO exposes the essential, non-sensitive state and location of a player entity.
 */
public record PlayerEntityDTO(
    Long id,
    Long playerId,  // ID of the associated Player (fetched via Player.getId())
    Long worldId,   // ID of the associated World (fetched via World.getId())
    String entityType,
    Integer chunkX,
    Integer chunkY,
    Float posX,
    Float posY,
    Integer health,
    String stateData,
    Instant createdAt
) {
    /**
     * Static factory method to map from the JPA Entity to the DTO.
     * * NOTE: This method relies on the associated Player and World objects being 
     * available (or at least having their IDs available) when the DTO is created.
     * * @param entity The source PlayerEntity object.
     * @return A new immutable PlayerEntityDTO instance.
     */
    public static PlayerEntityDTO fromEntity(PlayerEntity entity) {
        return new PlayerEntityDTO(
            entity.getId(),
            // Access the ID from the related entity object
            entity.getPlayer().getId(), 
            // Access the ID from the related entity object
            entity.getWorld().getId(),  
            entity.getEntityType(),
            entity.getChunkX(),
            entity.getChunkY(),
            entity.getPosX(),
            entity.getPosY(),
            entity.getHealth(),
            entity.getStateData(),
            entity.getCreatedAt()
		);
	}
}
package com.cosmic.scavengers.networking.commands.handlers.binary;

import org.decimal4j.api.Decimal;
import org.decimal4j.scale.Scale4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.core.commands.ICommandBinaryHandler;
import com.cosmic.scavengers.db.services.jooq.PlayerInitService;
import com.cosmic.scavengers.gameplay.services.entities.EntityActionService;
import com.cosmic.scavengers.gameplay.services.entities.data.MoveRequestData;
import com.cosmic.scavengers.networking.commands.NetworkBinaryCommand;
import com.cosmic.scavengers.networking.constants.meta.NetworkAttributeKeys;
import com.cosmic.scavengers.utils.DecimalUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

@Component
public class EntityMoveCommandHandler implements ICommandBinaryHandler {
	private static final Logger log = LoggerFactory.getLogger(EntityMoveCommandHandler.class);

	private final EntityActionService entityActionService;	

	public EntityMoveCommandHandler(EntityActionService entityActionService) {
		this.entityActionService = entityActionService;
	}

	@Override
	public NetworkBinaryCommand getCommand() {
		return NetworkBinaryCommand.REQUEST_ENTITY_MOVE_C;
	}

	@Override
	public void handle(ChannelHandlerContext ctx, ByteBuf payload) {
		log.info("Handling {} command for channel {}.", 
				getCommand().getLogText(), ctx.channel().id());

		// (8) bytes for EntityID + (6 * 8) bytes for decimals = 56 bytes
		if (payload.readableBytes() < 56) {
			log.error("Malformed move command from {}: expected 56 bytes, got {}", 
					ctx.channel().id(), payload.readableBytes());
			return;
		}

		final Long playerId = 
				ctx.channel().attr(NetworkAttributeKeys.PLAYER_ID_KEY.<Long>getKey()).get();
		if (playerId == null) {
			log.error("Unauthorized move request: No PlayerID associated with channel {}.", ctx.channel().id());
			ctx.close(); // Immediate disconnect for security if session is corrupted
			return;
		}

		final MoveRequestData moveRequestData = getMoveRequestData(playerId, payload);
		entityActionService.processMoveRequest(moveRequestData);
	}

	private MoveRequestData getMoveRequestData(Long playerId, ByteBuf payload) {
		final long entityId = payload.readLong();

		final long scaledX = payload.readLong();
		final long scaledY = payload.readLong();
		final long scaledZ = payload.readLong();

		final Decimal<Scale4f> targetX = DecimalUtils.fromScaled(scaledX);
		final Decimal<Scale4f> targetY = DecimalUtils.fromScaled(scaledY);
		final Decimal<Scale4f> targetZ = DecimalUtils.fromScaled(scaledZ);

		final long scaledMovementSpeed = payload.readLong();
		final long scaledRotationSpeed = payload.readLong();
		final long scaledStoppingDistance = payload.readLong();

		final Decimal<Scale4f> unscaledMovementSpeed = DecimalUtils.fromScaled(scaledMovementSpeed);
		final Decimal<Scale4f> unscaledRotationSpeed = DecimalUtils.fromScaled(scaledRotationSpeed);
		final Decimal<Scale4f> unscaledStoppingDistance = DecimalUtils.fromScaled(scaledStoppingDistance);

		log.info("Constructed new MoveRequestData: PlayerId: '{}' requested move of EntityId: '{}' to Target: [{}, {}, {}] - MovRotDist: [{} {} {}]",
				playerId, entityId, 
				targetX, targetY, targetZ,
				unscaledMovementSpeed, unscaledRotationSpeed, scaledStoppingDistance);

		return new MoveRequestData(
				entityId, playerId,
				targetX, targetY, targetZ, 
				unscaledMovementSpeed, unscaledRotationSpeed, unscaledStoppingDistance);
	}
}

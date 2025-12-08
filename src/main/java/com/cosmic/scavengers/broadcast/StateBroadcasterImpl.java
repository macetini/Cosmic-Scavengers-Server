package com.cosmic.scavengers.broadcast;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cosmic.scavengers.component.Movement;
import com.cosmic.scavengers.component.Position;
import com.cosmic.scavengers.networking.dto.PositionUpdateDto;

import dev.dominion.ecs.api.Dominion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

/**
 * Implementation of IStateBroadcaster. Responsible for collecting position data
 * from the ECS, serializing it into a binary ByteBuf, and sending it to
 * clients.
 */
@Service
public class StateBroadcasterImpl implements IStateBroadcaster {	
	private static final Logger log = LoggerFactory.getLogger(StateBroadcasterImpl.class);
	// This broadcaster is assumed to manage the list of GameChannelHandlers
	private IMessageBroadcaster messageBroadcaster;

	/**
	 * Setter method used by Spring to inject the dependency after the bean is
	 * constructed. This breaks the constructor cycle.
	 */
	@Autowired // This tells Spring to call this method immediately after construction
	public void setMessageBroadcaster(IMessageBroadcaster messageBroadcaster) {
		this.messageBroadcaster = messageBroadcaster;
	}
	
	@Override
	public void broadcastCurrentState(Dominion dominion) {
		List<PositionUpdateDto> updates = dominion.findEntitiesWith(Position.class, Movement.class).stream()
				.map(result -> new PositionUpdateDto(						
						result.entity().getName(), result.comp1().x().unscaledValue(),
						result.comp1().y().unscaledValue()))
				.toList();

		if (updates.isEmpty()) {
			//log.info("No entities to broadcast.");
			return;
		}

		ByteBuf binaryPayload = serializePositionUpdates(updates);
		messageBroadcaster.broadcastBinary(binaryPayload);
	}

	/**
	 * Serializes a list of position updates into a single Netty ByteBuf. The
	 * structure is [int count] followed by N * [long id, long x, long y].
	 */
	private ByteBuf serializePositionUpdates(List<PositionUpdateDto> updates) {
		ByteBuf buffer = Unpooled.buffer(4096);
		buffer.writeInt(updates.size());

		for (PositionUpdateDto update : updates) {
			byte[] nameBytes = update.entityName.getBytes(CharsetUtil.UTF_8);
			buffer.writeInt(nameBytes.length);
			
			buffer.writeBytes(nameBytes);
			
			buffer.writeLong(update.unscaledX);
			buffer.writeLong(update.unscaledY);
		}

		return buffer;
	}
}
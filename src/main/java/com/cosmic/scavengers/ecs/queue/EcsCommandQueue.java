package com.cosmic.scavengers.ecs.queue;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.cosmic.scavengers.ecs.commands.meta.IEcsCommand;

@Component
public class EcsCommandQueue {
	private final ConcurrentLinkedQueue<IEcsCommand> queue = new ConcurrentLinkedQueue<>();

	public void submit(IEcsCommand request) {
		queue.add(request);
	}

	public IEcsCommand poll() {
		return queue.poll();
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}
}
package com.cosmic.scavengers.networking.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.services.jooq.PlayerInitService;
import com.cosmic.scavengers.services.jooq.UserService;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Placeholder for the Netty server setup and binding.
 */
@Component
public class NettyServer implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
	private static final int PORT = 8080;

	// Dependencies
	private final UserService userService;
	private final PlayerInitService playerInitService;

	public NettyServer(UserService userService, PlayerInitService playerInitService) {
		this.userService = userService;
		this.playerInitService = playerInitService;
	}

	@Override
	public void run() {
		EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
		EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

		try {
			ServerBootstrap serverBootstrap = new ServerBootstrap();
			serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new NettyServerInitializer(userService, playerInitService));

			serverBootstrap.bind(PORT).sync().channel().closeFuture().sync();
			log.info("Netty Server started and listening on port {}", PORT);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
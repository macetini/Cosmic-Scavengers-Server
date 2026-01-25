package com.cosmic.scavengers.core.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cosmic.scavengers.db.services.UserService;
import com.cosmic.scavengers.networking.CommandRouter;

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

	private final CommandRouter networkDispatcher;	

	public NettyServer(CommandRouter networkDispatcher, UserService userService) {
		this.networkDispatcher = networkDispatcher;		
	}

	@Override
	public void run() {
		EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
		EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

		try {
			ServerBootstrap serverBootstrap = new ServerBootstrap();
			serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new NettyServerInitializer(networkDispatcher));

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
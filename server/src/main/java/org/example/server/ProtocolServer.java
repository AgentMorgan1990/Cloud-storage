package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.handler.ProtocolInboundHandler;


//todo добавить функционал удаления файлов/папок и создания папок на серваке
//todo отрефачить код в клиенте
//todo отрефачить код на серваке
//todo добавть авторизацию пока с реализацией в массиве на серваке
//todo добавить авторизацию с БД
//todo прогресс бар на загрузку файла
//todo прогресс бар на скачивание файла

public class ProtocolServer {

    public void run() throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel serverChannel) {
                            serverChannel.pipeline().addLast(new ProtocolInboundHandler());
                        }
                    });
            //todo magic number
            ChannelFuture channelFuture = b.bind(9090).sync();
            channelFuture.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new ProtocolServer().run();
    }
}

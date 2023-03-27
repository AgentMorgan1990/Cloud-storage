package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.handler.ProtocolHandler;




//todo корректно переименовать загрузку выгрузку
//todo подумать над корректныой реализаций стейтов
//todo подумать над разделением и подсчётом посылок
//todo доработать клиентскую часть по визуализации выгрузки-загрузки (два экрана и кнопки)
//todo отображение файлов на серверном хранилище

//todo авторизация клиентская часть
//todo авторизация серверная часть (раскатываеем БД в докере?)
//todo движение по папкам вверх-вниз, отбражение файлов и папок

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
                            serverChannel.pipeline().addLast(new ProtocolHandler());
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

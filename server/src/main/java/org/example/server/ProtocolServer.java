package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.handler.ProtocolInboundHandler;


//todo MVP1 - рефакторинг кода после получения этого функционала
//todo Разобраться с реализацией callBack-ов и красивее передавать его, в контроллера какая-то каша

//todo корректно переименовать загрузку выгрузку
//todo подумать над корректныой реализаций стейтов
//todo зашить логик клиента в протокол - для авторизации
//todo для авторизации на серваке создать отдельный хэндлер - например первый,
// а протом можно прокидывать во второй уже с обработкой

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

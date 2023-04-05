package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.handler.ProtocolInboundHandler;

//баги
//todo обновлять список отображения файлов ан клиенте, когда скачали новы файл с сервака (callback) ++DONE
//todo протестить, как имплементированный сервис будет работать в многопотоке, кажется есть вероятность ошибок, возможно изменить на решение с хэндлерами
//todo сохранять на клиенте в ту папку, где сейчас находимся

//фичи
//todo добавть авторизацию пока с реализацией в массиве на серваке
//todo добавить авторизацию с БД
//todo прогресс бар на загрузку файла
//todo прогресс бар на скачивание файла
//todo удаление папки, даже если в ней есть файлы
//todo возможность убрать строку с файлами, а вывести возможность вводить название файлов через диалоговое окно
//todo настроить в проекте в запаковку в исполняемый файл

//рефакторинг
//todo рефакторинг код на серваке (продумать о сокращении стейтов, навигация и создание/удаление используют схожие методы по получению имени файла)
//todo рефакторинг netty добавить код с работой с несколькими хэндлерами
//todo рефакторинг netty добавить пример работы с outboundHandler-ом
//todo рефакторинг общий на клиенте
//todo рефакторинг общий на серваке
//todo рефакторинг добавить второй хэндлер на серваке, который будет реализовывать логику, например только чтения файла, разбить логику первого хэндлера на несколько частей

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

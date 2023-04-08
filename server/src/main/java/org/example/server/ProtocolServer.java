package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.handler.AuthHandler;

//баги


//фичи
//todo добавить запаковку с зависимостями, чтобы можно было запускать отдельные приложения сервака и клиента
//todo добавить авторизацию с БД
//todo прогресс бар на загрузку файла
//todo прогресс бар на скачивание файла
//todo удаление папки, даже если в ней есть файлы
//todo возможность убрать строку с файлами, а вывести возможность вводить название файлов через диалоговое окно

//рефакторинг
//todo рефакторинг возможность вынести отработку длины файла, длины команд и команд в отдельный хэндлер или сервис
//todo рефакторинг netty добавить пример работы с outboundHandler-ом
//todo рефакторинг общий на клиенте
//todo рефакторинг общий на серваке
//todo рефакторинг добавить второй хэндлер на серваке, который будет реализовывать логику, например только чтения файла, разбить логику первого хэндлера на несколько частей
//Чтение размера пакета и команды -> прокидываем в Авторизацию (если есть) -> прокидываем в исполнение команд


/**
 * для авторизации может потребоваться следующая конструкция для запоминания клиентов,добавлять можно при подключении к
 * каналу, вроде должно автоматически чистится при отключении
 *
 *  ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
 *  channels.add(channelFuture.channel());
 *
 *
 */


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
                            serverChannel.pipeline().addLast(new AuthHandler());
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

package ru.example.chat;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

public class Network {

    private static Network INSTANCE;

    public static Network getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Network();
        }
        return INSTANCE;
    }

    private Network() {}

    private Channel currentChannel;

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void setOnReceivedFileCallback(Callback callback) {
        currentChannel.pipeline().get(ProtocolInboundHandler.class).setCallbackOnReceivedFile(callback);
    }

    public void setOnReceivedFileListCallback(Callback callback){
        currentChannel.pipeline().get(ProtocolInboundHandler.class).setCallbackOnReceivedFileList(callback);
    }

    public void setOnAuthorizationPass(Callback callback){
        currentChannel.pipeline().get(ProtocolInboundHandler.class).setCallbackOnAuthorizationPass(callback);
    }

    public void setOnAuthorizationFailed(Callback callback){
        currentChannel.pipeline().get(ProtocolInboundHandler.class).setCallbackOnAuthorizationFailed(callback);
    }

    public void setCurrentDir(Path currentDir){
        protocolInboundHandler.setCurrentDir(currentDir);
    }
    ProtocolInboundHandler protocolInboundHandler;



    public void start(CountDownLatch countDownLatch,Path currentDir) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress("localhost", 9090))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(protocolInboundHandler = new ProtocolInboundHandler());
                            currentChannel = socketChannel;
                        }
                    });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            countDownLatch.countDown();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        currentChannel.close();
    }
}

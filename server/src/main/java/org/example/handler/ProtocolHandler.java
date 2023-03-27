package org.example.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.example.FileService;
import org.example.model.Commands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProtocolHandler extends ChannelInboundHandlerAdapter {
   private enum State { IDLE, READ_PACKAGE_SIZE, READ_COMMAND, EXECUTE_COMMAND }
    private State currentState = State.IDLE;
    private FileService fileService = new FileService();
    private long packageSize;
    private long redSize;
    private Commands command;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {

        packageSize = 0L;
        redSize = 0L;

        ByteBuf buf = ((ByteBuf) msg);

        while (buf.readableBytes() > 0) {

            if (currentState.equals(State.IDLE) && buf.readableBytes() >= 8) {
                packageSize = buf.readLong();
                System.out.println("State: " + State.READ_PACKAGE_SIZE + " packageSize: " + packageSize);
                redSize += 8;
                currentState = State.READ_PACKAGE_SIZE;
            }

            if (currentState.equals(State.READ_PACKAGE_SIZE) && buf.readableBytes() >= 4) {
                byte[] commandTitle = new byte[buf.readInt()];
                redSize += 4 + commandTitle.length;
//                redSize += commandTitle.length;
                buf.readBytes(commandTitle);
                command = Commands.valueOf(new String(commandTitle, StandardCharsets.UTF_8));
                System.out.println("State: " + State.READ_COMMAND + " command: " + command);
                currentState = State.READ_COMMAND;
            }

            if (currentState.equals(State.READ_COMMAND)) {

                currentState = State.EXECUTE_COMMAND;

                switch (command) {

                    case UPLOAD_FILE:
                        //todo дописываем метод скачивания файла -> тут как раз можно перекинуть всё в другой буфер
                        System.out.println("Надо загрузить файл");
                        currentState = State.IDLE;
                        break;

                    case DOWNLOAD_FILE:
                        fileService.downloadFile(buf,packageSize,redSize);
                        System.out.println("Надо отправить файл");
                        currentState = State.IDLE;
                        break;

                    case CRAZY_MESSAGE:
                        System.out.println("Кажется пользователь хочет пообщаться");
                        currentState = State.IDLE;
                        break;

                    case AUTHORIZATION:
                        System.out.println("Надо авторизовать пользователя");
                        currentState = State.IDLE;
                        break;
                }
            }
        }


        if (buf.readableBytes() == 0) {
            System.out.println("Освободили Буф");
            buf.release();

        }
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
       cause.printStackTrace();
        ctx.close();
    }
}

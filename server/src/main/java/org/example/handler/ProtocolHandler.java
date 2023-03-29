package org.example.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.example.model.Commands;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProtocolHandler extends ChannelInboundHandlerAdapter {
   private enum State {IDLE, READ_PACKAGE_SIZE, READ_COMMAND, EXECUTE_COMMAND ,NAME_LENGTH, NAME, FILE_LENGTH, FILE, SENT_FILE}
    private State currentState = State.IDLE;
    private long packageSize;
    private long redSize;
    private Commands command;
    private int nextLength;
    private long fileLength;
    private BufferedOutputStream out;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {

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
                buf.readBytes(commandTitle);
                command = Commands.valueOf(new String(commandTitle, StandardCharsets.UTF_8));
                System.out.println("State: " + State.READ_COMMAND + " command: " + command);
                currentState = State.READ_COMMAND;
            }


            if (currentState.equals(State.READ_COMMAND)) {

                switch (command) {
                    case DOWNLOAD_FILE:
                        currentState = State.NAME_LENGTH;
                        break;

                    case UPLOAD_FILE:
                        currentState = State.SENT_FILE;
                        //todo перекинуть в хэндлер отправки файлов
                        break;
                }
            }


            if (currentState == State.NAME_LENGTH && buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                redSize += 4;

                currentState = State.NAME;
                System.out.println("State: " + State.NAME + " nextLength: " + nextLength);
            }

            if (currentState == State.NAME && buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                redSize += nextLength;
                System.out.println("STATE: Filename received - _" + new String(fileName, "UTF-8"));
                out = new BufferedOutputStream(new FileOutputStream("_" + new String(fileName)));
                currentState = State.FILE_LENGTH;
            }

            if (currentState == State.FILE_LENGTH && buf.readableBytes() >= 8) {
                fileLength = buf.readLong();
                redSize += 8;
                System.out.println("STATE: File length received - " + fileLength);
                currentState = State.FILE;
            }


            //todo есть ли вероятность, что-то останется в буфе и мы повиснем в цикле?

            if (currentState == State.FILE) {
//                System.out.println("Количество непрочитанных байт в буфе: " + buf.readableBytes() + " packageSize: " + packageSize + " redSize: "+ redSize);
                redSize++;
//                System.out.println("State: " + State.FILE);
                out.write(buf.readByte());
            }
        }

        if (currentState == State.FILE && packageSize == redSize) {
            System.out.println("File received");
            out.close();
            packageSize = 0L;
            redSize = 0L;
            currentState = State.IDLE;
            System.out.println("State: " +  currentState);
        }


        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
       cause.printStackTrace();
        ctx.close();
    }
}

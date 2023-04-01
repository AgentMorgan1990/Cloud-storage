package ru.example.chat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.example.model.Commands;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProtocolInboundHandler extends ChannelInboundHandlerAdapter {
   private enum State {IDLE, READ_PACKAGE_SIZE, READ_COMMAND, EXECUTE_COMMAND ,NAME_LENGTH, NAME, FILE_LENGTH, FILE, SENT_FILE, SENT_REMOTE_FILE_LIST, LIST_LENGTH, FILE_LIST}
    private State currentState = State.IDLE;
    private long packageSize;
    private long redSize;
    private Commands command;
    private int nextLength;
    private long fileLength;
    private long listLength;
    private BufferedOutputStream out;

    private ExecutorService executorService;
    private Callback callback;

    public ProtocolInboundHandler(Callback callback) {
        this.callback = callback;
        executorService = Executors.newSingleThreadExecutor();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, InterruptedException, ClassNotFoundException {

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
                        System.out.println("Получили команду на скачивание");
                        currentState = State.NAME_LENGTH;
                        break;

                    case UPLOAD_FILE:
                        currentState = State.SENT_FILE;
                        //todo перекинуть в хэндлер отправки файлов
                        break;
                    case REFRESH_REMOTE_FILE_LIST:
                        currentState = State.SENT_REMOTE_FILE_LIST;
                        break;
                    case SENT_FILE_LIST:
                        System.out.println("Получили лист");
                        currentState = State.LIST_LENGTH;
                }
            }

            if (currentState.equals(State.LIST_LENGTH)&& buf.readableBytes() >= 8){
                listLength = buf.readLong();
                redSize += 8;
                System.out.println("STATE: List length received - " + listLength);
                currentState = State.FILE_LIST;
            }

            if (currentState.equals(State.FILE_LIST) && buf.readableBytes() >= listLength){

                System.out.println("STATE: FILE_LIST");
                byte[] fileList = new byte[(int)listLength];
                System.out.println("Создали массив");
                buf.readBytes(fileList);


                ByteArrayInputStream b = new ByteArrayInputStream(fileList);
                ObjectInputStream o = new ObjectInputStream(b);
                List<String> list = (List<String>) o.readObject();

                System.out.println(list);
                callback.call(list);

                currentState = State.IDLE;
                redSize=0L;
                packageSize = 0L;
                System.out.println("State: "+ State.IDLE);
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
                out = new BufferedOutputStream(new FileOutputStream("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/client_storage/"+"Файл пишет клиент _" + new String(fileName)));
                //todo колбэк на обновление файлов на клиенте
                currentState = State.FILE_LENGTH;
            }

            if (currentState == State.FILE_LENGTH && buf.readableBytes() >= 8) {
                fileLength = buf.readLong();
                redSize += 8;
                System.out.println("STATE: File length received - " + fileLength);
                currentState = State.FILE;
            }


            if (currentState == State.FILE && buf.readableBytes()>0) {
                System.out.println("State: "+ currentState);
                redSize++;
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

package org.example.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.example.model.Commands;
import org.example.server.SentService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ProtocolInboundHandler extends ChannelInboundHandlerAdapter {
   private enum State {IDLE, READ_PACKAGE_SIZE, READ_COMMAND, EXECUTE_COMMAND ,NAME_LENGTH, NAME, FILE_LENGTH, FILE, READ_SEND_FILE_NAME_LENGTH, READ_SEND_FILE_NAME, SEND_FILE, SENT_REMOTE_FILE_LIST}
    private State currentState = State.IDLE;
    private long packageSize;
    private long redSize;
    private Commands command;
    private int nextLength;
    private long fileLength;
    private String fileName;
    private BufferedOutputStream out;

    private ExecutorService executorService;

    public ProtocolInboundHandler() {
        executorService = Executors.newSingleThreadExecutor();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {

        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            System.out.println("Есть что читать");

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
                        currentState = State.READ_SEND_FILE_NAME_LENGTH;
                        //todo перекинуть в хэндлер отправки файлов
                        break;
                    case REFRESH_REMOTE_FILE_LIST:
                        currentState = State.SENT_REMOTE_FILE_LIST;
                        break;
                }
            }


            if (currentState.equals(State.READ_SEND_FILE_NAME_LENGTH) && buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                redSize += 4;

                currentState = State.READ_SEND_FILE_NAME;
                System.out.println("State: " + State.READ_SEND_FILE_NAME_LENGTH + " nextLength: " + nextLength);
            }


            if (currentState.equals(State.READ_SEND_FILE_NAME) && buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                this.fileName = new String(fileName, "UTF-8");
                redSize += nextLength;
                currentState = State.SEND_FILE;
            }

            if (currentState.equals(State.SEND_FILE)) {
                System.out.println("STATE: SEND_FILE");
                executorService.execute(() -> {

                    System.out.println(fileName);
                    Path path = Paths.get("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/server_storage/" + fileName);

                    long fileSize;
                    try {
                        fileSize = Files.size(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    FileRegion region = new DefaultFileRegion(path.toFile(), 0, fileSize);
                    byte[] commandName = Commands.DOWNLOAD_FILE.toString().getBytes(StandardCharsets.UTF_8);
                    byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);

                    long packageSize = 0L;

                    packageSize += 8;                       // long длина посылки
                    packageSize += 4;                       // int длина наименования команды
                    packageSize += commandName.length;      // наименование команды
                    packageSize += 4;                       // int длина имени файла
                    packageSize += filenameBytes.length;    // имя файла
                    packageSize += 8;                       // long длина файла
                    packageSize += fileSize;                // файл

                    System.out.println("Отправлен файл с размером: "+ packageSize);
                    System.out.println("Размер файла в посылке " + fileSize);

                    ByteBuf bufWriteFile = null;
                    bufWriteFile = ByteBufAllocator.DEFAULT.directBuffer(1);
                    bufWriteFile.writeLong(packageSize);
                    bufWriteFile.writeInt(commandName.length);
                    bufWriteFile.writeBytes(commandName);
                    bufWriteFile.writeInt(filenameBytes.length);
                    bufWriteFile.writeBytes(filenameBytes);
                    bufWriteFile.writeLong(fileSize);

                    ctx.channel().writeAndFlush(bufWriteFile);

                    System.out.println("Отправили данные по файлу");

                    ctx.channel().writeAndFlush(region);

                    System.out.println("Отправили файл");
                    redSize =0L;
                    currentState = State.IDLE;
                    System.out.println(currentState);
                });

            }


            if (currentState.equals(State.SENT_REMOTE_FILE_LIST)) {

                executorService.execute(() -> {
                    List<String> filesList = null;
                    try {
                        filesList = Files.list(Paths.get("server_storage"))
                                .sorted((o1, o2) -> {
                                    if (Files.isDirectory(o1) && !Files.isDirectory(o2)) {
                                        return -1;
                                    } else if (!Files.isDirectory(o1) && Files.isDirectory(o2)) {
                                        return 1;
                                    } else return 0;
                                })
                                .map(p -> {
                                    if (!Files.isDirectory(p)) {
                                        return p.getFileName().toString();
                                    } else {
                                        return "[" + p.getFileName().toString() + "]";
                                    }
                                })
                                .collect(Collectors.toList());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    byte[] commandName = Commands.SENT_FILE_LIST.toString().getBytes(StandardCharsets.UTF_8);
                    byte[] fileList1 = new byte[0];

                    try {
                        ByteArrayOutputStream b = new ByteArrayOutputStream();
                        ObjectOutputStream o = new ObjectOutputStream(b);
                        o.writeObject(filesList);
                        fileList1 = b.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    long packageSize = 0L;

                    packageSize += 8;                       // long длина посылки
                    packageSize += 4;                       // int длина наименования команды
                    packageSize += commandName.length;      // наименование команд
                    packageSize += 8;                       // long длина файла
                    packageSize += fileList1.length;                // файл

                    System.out.println(packageSize);
                    System.out.println(fileList1);


//
                    ByteBuf bufWrite = null;
                    bufWrite = ByteBufAllocator.DEFAULT.directBuffer(1);
                    bufWrite.writeLong(packageSize);
                    bufWrite.writeInt(commandName.length);
                    bufWrite.writeBytes(commandName);
                    bufWrite.writeLong(fileList1.length);
                    bufWrite.writeBytes(fileList1);

                    ctx.channel().writeAndFlush(bufWrite);

                    redSize = 0L;
                    currentState = State.IDLE;
                });
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
                out = new BufferedOutputStream(new FileOutputStream("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/server_storage/"+"Файл пишет сервак _" + new String(fileName)));
                currentState = State.FILE_LENGTH;
            }

            if (currentState == State.FILE_LENGTH && buf.readableBytes() >= 8) {
                fileLength = buf.readLong();
                redSize += 8;
                System.out.println("STATE: File length received - " + fileLength);
                currentState = State.FILE;
            }


            if (currentState == State.FILE && buf.readableBytes()>0) {
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
                System.out.println("State: " + currentState);

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

package org.example.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SentService {

    private ExecutorService executorService;
    private static final Logger log = LogManager.getLogger(SentService.class);

    public SentService() {

        executorService = Executors.newSingleThreadExecutor();
    }


    public void sendFileList(Path currentDir, ChannelHandlerContext ctx, ChannelFutureListener finishListener) {

        executorService.execute(() -> {
            List<String> filesList = null;
            try {
                filesList = Files.list(Paths.get(currentDir.toString()))
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
                                return "[dir] " + p.getFileName().toString();
                            }
                        })
                        .collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] commandName = Commands.REFRESH_SERVER_FILE_AND_DIRECTORY_LIST.toString().getBytes(StandardCharsets.UTF_8);
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
            packageSize += fileList1.length;        // файл

            log.info("Send remote file list. Package size: " + packageSize);

            ByteBuf bufWrite = null;
            bufWrite = ByteBufAllocator.DEFAULT.directBuffer(1);
            bufWrite.writeLong(packageSize);
            bufWrite.writeInt(commandName.length);
            bufWrite.writeBytes(commandName);
            bufWrite.writeLong(fileList1.length);
            bufWrite.writeBytes(fileList1);

            ChannelFuture transferOperationFuture = ctx.channel().writeAndFlush(bufWrite);

            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });
    }


    public void sendFile(Path currentDir, String fileName, ChannelHandlerContext ctx, ChannelFutureListener finishListener) {

        executorService.execute(() -> {

            Path path = Paths.get(currentDir.toString() + "/" + fileName);

            long fileSize;
            try {
                fileSize = Files.size(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            FileRegion region = new DefaultFileRegion(path.toFile(), 0, fileSize);
            byte[] commandName = Commands.SEND_FILE_FROM_SERVER.toString().getBytes(StandardCharsets.UTF_8);
            byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);

            long packageSize = 0L;

            packageSize += 8;                       // long длина посылки
            packageSize += 4;                       // int длина наименования команды
            packageSize += commandName.length;      // наименование команды
            packageSize += 4;                       // int длина имени файла
            packageSize += filenameBytes.length;    // имя файла
            packageSize += 8;                       // long длина файла
            packageSize += fileSize;                // файл

            log.info("Send remote file. Package size: " + packageSize + ". File size: " + fileSize);

            ByteBuf bufWriteFile = null;
            bufWriteFile = ByteBufAllocator.DEFAULT.directBuffer(1);
            bufWriteFile.writeLong(packageSize);
            bufWriteFile.writeInt(commandName.length);
            bufWriteFile.writeBytes(commandName);
            bufWriteFile.writeInt(filenameBytes.length);
            bufWriteFile.writeBytes(filenameBytes);
            bufWriteFile.writeLong(fileSize);
            ctx.channel().writeAndFlush(bufWriteFile);

            ChannelFuture transferOperationFuture = ctx.channel().writeAndFlush(region);
            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });
    }

    public void sendCommand(ChannelHandlerContext ctx, Commands command) {
        executorService.execute(() -> {

            byte[] commandName = command.toString().getBytes(StandardCharsets.UTF_8);

            long packageSize = 0L;
            packageSize += 8;                       // long длина посылки
            packageSize += 4;                       // int длина наименования команды
            packageSize += commandName.length;      // наименование команды


            log.info("Send remote file. Package size: " + packageSize + ". Command size: " + command);

            ByteBuf bufWriteFile = null;
            bufWriteFile = ByteBufAllocator.DEFAULT.directBuffer(1);
            bufWriteFile.writeLong(packageSize);
            bufWriteFile.writeInt(commandName.length);
            bufWriteFile.writeBytes(commandName);
            ctx.channel().writeAndFlush(bufWriteFile);

        });
    }
}

package org.example.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SentService {

    private ExecutorService executorService;
    private static final Logger log = LogManager.getLogger(SentService.class);

    public SentService() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public void sendMessage(Channel channel, Command command, byte[] message, ChannelFutureListener finishListener) {
        executorService.execute(() -> {
            byte[] commandArr = command.toString().getBytes(StandardCharsets.UTF_8);

            long packageSize = 0L;
            packageSize += 8;                               // long длина посылки
            packageSize += 4;                               // int длина наименования команды
            packageSize += commandArr.length;               // наименование команд
            if (message != null) {
                packageSize += 4;                           // long длина сообщения
                packageSize += message.length;           // сообщение
            }
            log.info("Send remote file list. Package size: " + packageSize);

            ByteBuf bufWrite = null;
            bufWrite = ByteBufAllocator.DEFAULT.directBuffer(1);
            bufWrite.writeLong(packageSize);
            bufWrite.writeInt(commandArr.length);
            bufWrite.writeBytes(commandArr);
            if (message != null) {
                log.info("Есть сообщение");
                log.error(message);
                bufWrite.writeInt(message.length);
                bufWrite.writeBytes(message);
            }
            ChannelFuture transferOperationFuture = channel.writeAndFlush(bufWrite);
            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });
    }

    public void sendFile(Command command,Path currentDir, String fileName, Channel channel, ChannelFutureListener finishListener) {

        executorService.execute(() -> {

            Path path = Paths.get(currentDir.toString() + "/" + fileName);

            long fileSize;
            try {
                fileSize = Files.size(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            FileRegion region = new DefaultFileRegion(path.toFile(), 0, fileSize);
            byte[] commandName = command.toString().getBytes(StandardCharsets.UTF_8);
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
            channel.writeAndFlush(bufWriteFile);

            ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });
    }
}

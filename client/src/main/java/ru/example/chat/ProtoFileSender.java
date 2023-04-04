package ru.example.chat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Commands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ProtoFileSender {

    private ExecutorService executorService;
    private static final Logger log = LogManager.getLogger(ProtoFileSender.class);

    public ProtoFileSender() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public void refreshRemoteFileList(Channel channel, ChannelFutureListener finishListener) {
        executorService.execute(() -> {
            long packageSize = 0L;
            byte[] commandName = Commands.REFRESH_SERVER_FILE_AND_DIRECTORY_LIST.toString().getBytes(StandardCharsets.UTF_8);

            packageSize += 8;
            packageSize += 4;
            packageSize += commandName.length;

            log.info("Send command: " + Commands.REFRESH_SERVER_FILE_AND_DIRECTORY_LIST + " . Package size: " + packageSize);

            ByteBuf buf = null;
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeLong(packageSize);
            buf.writeInt(commandName.length);
            buf.writeBytes(commandName);
            ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);

            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });
    }

    public void uploadFile(String fileName, Channel channel, ChannelFutureListener finishListener) {

        executorService.execute(() -> {

            long packageSize = 0L;
            byte[] commandName = Commands.SEND_FILE_FROM_SERVER.toString().getBytes(StandardCharsets.UTF_8);
            byte[] fileNameArr = fileName.getBytes();

            packageSize += 8;
            packageSize += 4;
            packageSize += commandName.length;
            packageSize += 4;
            packageSize += fileNameArr.length;

            log.info("Send command: " + Commands.SEND_FILE_FROM_SERVER + ". Package size: " + packageSize + ". File name: " + fileName);

            ByteBuf buf = null;
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeLong(packageSize);
            buf.writeInt(commandName.length);
            buf.writeBytes(commandName);
            buf.writeInt(fileNameArr.length);
            buf.writeBytes(fileNameArr);
            ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);


            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });
    }

    public void downFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
        executorService.execute(() -> {

            long fileSize;
            try {
                fileSize = Files.size(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            FileRegion region = new DefaultFileRegion(path.toFile(), 0, fileSize);
            byte[] commandName = Commands.SEND_FILE_TO_SERVER.toString().getBytes(StandardCharsets.UTF_8);
            byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);

            long packageSize = 0L;

            packageSize += 8;                       // long длина посылки
            packageSize += 4;                       // int длина наименования команды
            packageSize += commandName.length;      // наименование команды
            packageSize += 4;                       // int длина имени файла
            packageSize += filenameBytes.length;    // имя файла
            packageSize += 8;                       // long длина файла
            packageSize += fileSize;                // файл

            log.info("Send command: " + Commands.SEND_FILE_TO_SERVER + ". Package size: " + packageSize + ". File size: " + fileSize);

            ByteBuf buf = null;
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeLong(packageSize);
            buf.writeInt(commandName.length);
            buf.writeBytes(commandName);
            buf.writeInt(filenameBytes.length);
            buf.writeBytes(filenameBytes);
            buf.writeLong(fileSize);
            channel.writeAndFlush(buf);

            ChannelFuture transferOperationFuture = channel.writeAndFlush(region);

            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });
    }

    public void directoryUp(Channel channel, ChannelFutureListener finishListener) {
        executorService.execute(() -> {
            long packageSize = 0L;
            byte[] commandName = Commands.GO_TO_SERVER_PARENT_DIRECTORY.toString().getBytes(StandardCharsets.UTF_8);

            packageSize += 8;
            packageSize += 4;
            packageSize += commandName.length;

            log.info("Send command: " + Commands.GO_TO_SERVER_PARENT_DIRECTORY + ". Package size: " + packageSize);

            ByteBuf buf = null;
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeLong(packageSize);
            buf.writeInt(commandName.length);
            buf.writeBytes(commandName);
            ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);

            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });

    }

    public void goToDirectory(Channel channel, String substring, ChannelFutureListener finishListener) {
        executorService.execute(() -> {

            long packageSize = 0L;
            byte[] commandName = Commands.GO_TO_SERVER_DIRECTORY.toString().getBytes(StandardCharsets.UTF_8);
            byte[] fileNameArr = substring.getBytes();

            packageSize += 8;
            packageSize += 4;
            packageSize += commandName.length;
            packageSize += 4;
            packageSize += fileNameArr.length;

            log.info("Send command: " + Commands.GO_TO_SERVER_DIRECTORY + ". Package size: " + packageSize);

            ByteBuf buf = null;
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeLong(packageSize);
            buf.writeInt(commandName.length);
            buf.writeBytes(commandName);
            buf.writeInt(fileNameArr.length);
            buf.writeBytes(fileNameArr);
            ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);

            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });

    }

    public void deleteFileOrDirectory(String deletedFileOrDirectoryName, Channel channel, ChannelFutureListener finishListener) {

        executorService.execute(() -> {

            long packageSize = 0L;
            byte[] commandName = Commands.DELETE_FILE_OR_DIRECTORY_ON_SERVER.toString().getBytes(StandardCharsets.UTF_8);
            byte[] fileNameArr = deletedFileOrDirectoryName.getBytes();

            packageSize += 8;
            packageSize += 4;
            packageSize += commandName.length;
            packageSize += 4;
            packageSize += fileNameArr.length;

            log.info("Send command: " + Commands.DELETE_FILE_OR_DIRECTORY_ON_SERVER + ". Package size: " + packageSize + ". File or directory name: " + deletedFileOrDirectoryName);

            ByteBuf buf = null;
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeLong(packageSize);
            buf.writeInt(commandName.length);
            buf.writeBytes(commandName);
            buf.writeInt(fileNameArr.length);
            buf.writeBytes(fileNameArr);
            ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);


            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });
    }

    public void createDirectory(String creatingDirectoryName, Channel channel, ChannelFutureListener finishListener) {

        executorService.execute(() -> {

            long packageSize = 0L;
            byte[] commandName = Commands.CREATE_DIRECTORY_ON_SERVER.toString().getBytes(StandardCharsets.UTF_8);
            byte[] fileNameArr = creatingDirectoryName.getBytes();

            packageSize += 8;
            packageSize += 4;
            packageSize += commandName.length;
            packageSize += 4;
            packageSize += fileNameArr.length;

            log.info("Send command: " + Commands.CREATE_DIRECTORY_ON_SERVER + ". Package size: " + packageSize + ". Directory name: " + creatingDirectoryName);

            ByteBuf buf = null;
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeLong(packageSize);
            buf.writeInt(commandName.length);
            buf.writeBytes(commandName);
            buf.writeInt(fileNameArr.length);
            buf.writeBytes(fileNameArr);
            ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);


            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });
    }


}


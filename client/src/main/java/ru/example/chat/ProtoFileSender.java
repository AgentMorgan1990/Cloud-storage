package ru.example.chat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.example.model.Commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ProtoFileSender {

    private ExecutorService executorService;

    public ProtoFileSender() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public void uploadFile(Channel channel, ChannelFutureListener finishListener) {

        executorService.execute(() -> {

            long packageSize = 0L;
            byte[] commandName = Commands.UPLOAD_FILE.toString().getBytes(StandardCharsets.UTF_8);

            packageSize += 8;
            packageSize += 4;
            packageSize += commandName.length;

            System.out.println(packageSize);

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

    public void downFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
        executorService.execute(() -> {

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

            System.out.println(packageSize);

            ByteBuf buf = null;
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeLong(packageSize);
            buf.writeInt(commandName.length);
            buf.writeBytes(commandName);
            buf.writeInt(filenameBytes.length);
            buf.writeBytes(filenameBytes);
            buf.writeLong(fileSize);

            channel.writeAndFlush(buf);

            //todo тут наверное надо порубить на куски файл
            ChannelFuture transferOperationFuture = channel.writeAndFlush(region);

            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        });
    }
}


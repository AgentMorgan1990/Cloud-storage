//package org.example.handler;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.ByteBufAllocator;
//import io.netty.channel.*;
//import io.netty.util.concurrent.Future;
//import io.netty.util.concurrent.GenericFutureListener;
//import org.example.model.Commands;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class ProtocolOutboundHandler extends ChannelOutboundHandlerAdapter {
//
//    private ExecutorService executorService;
//
//    public ProtocolOutboundHandler() {
//        executorService = Executors.newSingleThreadExecutor();
//    }
//
////    @Override
////    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
////
////        String str = (String)msg;
////        byte[] arr = str.getBytes();
////        ByteBuf buf = ctx.alloc().buffer(arr.length);
////        buf.writeBytes(arr);
////        ctx.writeAndFlush(buf);
////        // buf.release(); // не выполняем release(), т.к. netty сам выполнит данную операцию при выполнении метода writeAndFlush()
////    }
//
//    @Override
//    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
//        executorService.execute(() -> {
//
//            long fileSize;
//            try {
//                fileSize = Files.size(path);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//            FileRegion region = new DefaultFileRegion(path.toFile(), 0, fileSize);
//            byte[] commandName = Commands.DOWNLOAD_FILE.toString().getBytes(StandardCharsets.UTF_8);
//            byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
//
//            long packageSize = 0L;
//
//            packageSize += 8;                       // long длина посылки
//            packageSize += 4;                       // int длина наименования команды
//            packageSize += commandName.length;      // наименование команды
//            packageSize += 4;                       // int длина имени файла
//            packageSize += filenameBytes.length;    // имя файла
//            packageSize += 8;                       // long длина файла
//            packageSize += fileSize;                // файл
//
//            System.out.println(packageSize);
//
//            ByteBuf buf = null;
//            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
//            buf.writeLong(packageSize);
//            buf.writeInt(commandName.length);
//            buf.writeBytes(commandName);
//            buf.writeInt(filenameBytes.length);
//            buf.writeBytes(filenameBytes);
//            buf.writeLong(fileSize);
//
//            ctx.writeAndFlush(buf);
//
//            ChannelFuture transferOperationFuture = ctx.writeAndFlush(region);
//
//            if (promise != null) {
//                transferOperationFuture.addListener((GenericFutureListener<? extends Future<? super Void>>) promise);
//            }
//        });
//    }
//}

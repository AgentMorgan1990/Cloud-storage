//package org.example;
//
//import io.netty.buffer.ByteBuf;
//import org.example.handler.ProtocolHandler;
//
//import java.io.BufferedOutputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//
//
//public class FileService {
//
//    public enum State {IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE}
//
//    private State currentState = State.NAME_LENGTH;
//    private int nextLength;
//    private long fileLength;
//    private long receivedFileLength;
//    private BufferedOutputStream out;
//    private int redByf;
//
//
//    public void downloadFile(ByteBuf buf, long packageSize, long redSize) throws IOException {
//
//        redByf = (int) redSize;
//
//        while (packageSize > redSize) {
//
//            System.out.println("packageSize: "+ packageSize + " redSize: "+ redSize);
//
//            if (currentState == State.NAME_LENGTH && buf.readableBytes() >= 4) {
//                nextLength = buf.readInt();
//                redSize += 4;
//                redByf+=4;
//                currentState = State.NAME;
//                System.out.println("State: " + State.NAME + " nextLength: " + nextLength);
//            }
//
//            if (currentState == State.NAME && buf.readableBytes() >= nextLength) {
//
//                byte[] fileName = new byte[nextLength];
//                buf.readBytes(fileName);
//                redSize += nextLength;
//                redByf+=nextLength;
//
//                System.out.println("STATE: Filename received - _" + new String(fileName, "UTF-8"));
//                out = new BufferedOutputStream(new FileOutputStream("_" + new String(fileName)));
//
//                currentState = State.FILE_LENGTH;
//            }
//
//            if (currentState == State.FILE_LENGTH && buf.readableBytes() >= 8) {
//                fileLength = buf.readLong();
//                redSize += 8;
//                redByf+=8;
//                System.out.println("STATE: File length received - " + fileLength);
//                currentState = State.FILE;
//            }
//
//
//            if (currentState == State.FILE && buf.readableBytes() > 0) {
//                System.out.println("Количество непрочитанных байт в буфе: "+ buf.readableBytes());
//                redSize++;
//                redByf++;
//                System.out.println("State: " + State.FILE);
//                out.write(buf.readByte());
//            }
//
//            if (redByf == 2048) {
//                System.out.println("Освободили Буф");
//                buf.clear();
////                buf.release();
//            }
//        }
//        currentState = State.NAME_LENGTH;
//        System.out.println("File received");
//        out.close();
//    }
//}

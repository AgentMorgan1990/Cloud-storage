package org.example.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Command;
import org.example.model.SentService;
import org.example.server.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProtocolInboundHandler extends ChannelInboundHandlerAdapter {
    private enum State {
        IDLE,
        READ_PACKAGE_SIZE,
        EXECUTE_COMMAND,
        READ_FILE_LENGTH,
        READ_FILE,
        GO_TO_PARENT_DIRECTORY,
        READ_STRING_LENGTH,
        READ_STRING,
        CREATE_FILE,
        SEND_FILE,
        GO_TO_DIRECTORY,
        DELETE_DIRECTORY,
        CREATE_DIRECTORY
    }

    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long redFileSize = 0L;
    private BufferedOutputStream out;
    private Path rootDir;
    private Path currentDir;
    private static final Logger log = LogManager.getLogger(ProtocolInboundHandler.class);
    private State nextState;
    private String nextString;
    private SentService sentService;


    public ProtocolInboundHandler(String login, ChannelHandlerContext ctx) {
        ctx.pipeline().removeFirst();

        File rootDir = new File("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/server_storage/" + login);
        if (!rootDir.exists()) rootDir.mkdirs();

        this.rootDir = rootDir.toPath();
        currentDir = rootDir.toPath();
        sentService = new SentService();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {

        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {

            if (currentState.equals(State.IDLE) && buf.readableBytes() > 0) {
                changeState(State.READ_PACKAGE_SIZE);
            }

            if (currentState.equals(State.READ_PACKAGE_SIZE) && buf.readableBytes() >= 8) {
                long packageSize = buf.readLong();
                log.info("State: " + currentState + " packageSize: " + packageSize);

                nextState = State.EXECUTE_COMMAND;
                changeState(State.READ_STRING_LENGTH);
            }

            if (currentState.equals(State.READ_STRING_LENGTH) && buf.readableBytes() >= 4) {
                readStringLength(buf);
                changeState(State.READ_STRING);
            }

            if (currentState.equals(State.READ_STRING) && buf.readableBytes() >= nextLength) {
                nextString = readString(buf);
                changeState(nextState);
            }

            if (currentState.equals(State.EXECUTE_COMMAND)) {

                switch (Command.valueOf(nextString)) {

                    case SEND_FILE_TO_SERVER:
                        nextState = State.CREATE_FILE;
                        changeState(State.READ_STRING_LENGTH);
                        break;

                    case SEND_FILE_FROM_SERVER:
                        nextState = State.SEND_FILE;
                        changeState(State.READ_STRING_LENGTH);
                        break;

                    case REFRESH_SERVER_FILE_AND_DIRECTORY_LIST:
                        sendServerFileList(ctx);
                        break;

                    case GO_TO_SERVER_PARENT_DIRECTORY:
                        changeState(State.GO_TO_PARENT_DIRECTORY);
                        break;

                    case GO_TO_SERVER_DIRECTORY:
                        nextState = State.GO_TO_DIRECTORY;
                        changeState(State.READ_STRING_LENGTH);
                        break;

                    case DELETE_FILE_OR_DIRECTORY_ON_SERVER:
                        nextState = State.DELETE_DIRECTORY;
                        changeState(State.READ_STRING_LENGTH);
                        break;

                    case CREATE_DIRECTORY_ON_SERVER:
                        nextState = State.CREATE_DIRECTORY;
                        changeState(State.READ_STRING_LENGTH);
                        break;
                }
            }

            if (currentState.equals(State.CREATE_DIRECTORY)) {
                File theDir = new File(currentDir + "/" + nextString);
                if (!theDir.exists()) theDir.mkdirs();
                log.info("State: " + currentState + " directory name: " + nextString);
                sendServerFileList(ctx);
            }

            if (currentState.equals(State.DELETE_DIRECTORY)) {
                Files.delete(Paths.get(currentDir + "/" + nextString));
                log.info("State: " + currentState + " directory name: " + nextString);
                sendServerFileList(ctx);
            }

            if (currentState.equals(State.GO_TO_DIRECTORY)) {
                currentDir = Paths.get(currentDir.toString() + "/" + nextString);
                log.info("State: " + currentState + " directory name: " + currentDir);
                sendServerFileList(ctx);
            }

            if (currentState.equals(State.GO_TO_PARENT_DIRECTORY)) {
                if (!rootDir.equals(currentDir)) currentDir = currentDir.getParent();
                log.info("State: " + currentState + " directory name: " + currentDir.toString());
                sendServerFileList(ctx);
            }

            if (currentState.equals(State.SEND_FILE)) {
                log.info("State: " + currentState + " nextLength: " + nextLength);
                sentService.sendFile(
                        Command.SEND_FILE_FROM_SERVER,
                        currentDir,
                        nextString,
                        ctx.channel(),
                        future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        changeState(State.IDLE);
                    }
                });
            }

            if (currentState.equals(State.CREATE_FILE)) {
                log.info("State: " + currentState + " Filename received: " + nextString);
                out = new BufferedOutputStream(Files.newOutputStream(Paths.get(currentDir + "/" + nextString)));
                changeState(State.READ_FILE_LENGTH);
            }

            if (currentState.equals(State.READ_FILE_LENGTH) && buf.readableBytes() >= 8) {
                fileLength = buf.readLong();
                log.info("State: " + currentState + " File length: " + fileLength);
                changeState(State.READ_FILE);
            }

            if (currentState.equals(State.READ_FILE) && buf.readableBytes() > 0) {
                out.write(buf.readByte());
                redFileSize++;
            }
        }


        if (currentState.equals(State.READ_FILE) && fileLength == redFileSize) {
            log.info("State: " + currentState + " file received");
            out.close();
            redFileSize = 0L;
            sendServerFileList(ctx);
        }

        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    private void changeState(State nextState) {
        State previousState = currentState;
        currentState = nextState;
        log.info("State: " + previousState + " ---> " + nextState);
    }

    private void readStringLength(ByteBuf buf) {
        nextLength = buf.readInt();
        log.info("State: " + currentState + " String length: " + nextLength);
    }


    private String readString(ByteBuf buf) {
        byte[] nameArr = new byte[nextLength];
        buf.readBytes(nameArr);
        String name = new String(nameArr, StandardCharsets.UTF_8);
        log.info("State: " + currentState + " String: " + name);
        return name;
    }

    private void sendServerFileList(ChannelHandlerContext ctx) {
        sentService.sendMessage(
                ctx.channel(),
                Command.REFRESH_SERVER_FILE_AND_DIRECTORY_LIST,
                Utils.getCurrentDirectoryContent(currentDir),
                future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        changeState(State.IDLE);
                    }
                });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}

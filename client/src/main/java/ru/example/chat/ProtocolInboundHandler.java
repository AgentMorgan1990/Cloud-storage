package ru.example.chat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Commands;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProtocolInboundHandler extends ChannelInboundHandlerAdapter {
    private enum State {
        IDLE,
        READ_PACKAGE_SIZE,
        READ_COMMAND,
        EXECUTE_COMMAND,
        READ_REMOTE_FILE_LIST_LENGTH,
        READ_REMOTE_FILE_LIST,
        DESERIALIZE_REMOTE_FILE_LIST,
        READ_FILE_NAME_LENGTH,
        READ_FILE_NAME,
        READ_FILE_LENGTH,
        READ_FILE
    }

    private Path currentDir;
    private State currentState = State.IDLE;
    private Commands command;
    private int nextLength;
    private long fileListLength;
    private long redFileLength = 0L;
    private long fileLength = 0L;
    private long redFileListLength = 0L;
    private BufferedOutputStream out;
    private byte[] fileList;
    private Callback callbackOnReceivedFile;
    private Callback callbackOnReceivedFileList;

    private Callback callbackOnAuthorizationPass;
    private Callback callbackOnAuthorizationFailed;
    private static final Logger log = LogManager.getLogger(ProtocolInboundHandler.class);

    public void setCurrentDir(Path currentDir){
        this.currentDir = currentDir;
    }

    public void setCallbackOnReceivedFileList(Callback callbackOnReceivedFileList){
        this.callbackOnReceivedFileList = callbackOnReceivedFileList;
    }

    public void setCallbackOnReceivedFile(Callback callbackOnReceivedFile) {
        this.callbackOnReceivedFile = callbackOnReceivedFile;
    }

    public void setCallbackOnAuthorizationPass(Callback callbackOnAuthorizationPass) {
        this.callbackOnAuthorizationPass = callbackOnAuthorizationPass;
    }
    public void setCallbackOnAuthorizationFailed(Callback callbackOnAuthorizationFailed) {
        this.callbackOnAuthorizationFailed = callbackOnAuthorizationFailed;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, ClassNotFoundException {

        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {

            if (currentState.equals(State.IDLE)&& buf.readableBytes() > 0){
                changeState(State.READ_PACKAGE_SIZE);
            }

            if (currentState.equals(State.READ_PACKAGE_SIZE) && buf.readableBytes() >= 8) {
                long packageSize = buf.readLong();
                log.info("State: " + currentState + " packageSize: " + packageSize);

                changeState(State.READ_COMMAND);
            }

            if (currentState.equals(State.READ_COMMAND) && buf.readableBytes() >= 4) {
                byte[] commandTitle = new byte[buf.readInt()];
                buf.readBytes(commandTitle);
                command = Commands.valueOf(new String(commandTitle, StandardCharsets.UTF_8));
                log.info("State: " + State.READ_COMMAND + " command: " + command);

                changeState(State.EXECUTE_COMMAND);
            }

            if (currentState.equals(State.EXECUTE_COMMAND)) {
                switch (command) {

                    case SEND_FILE_FROM_SERVER:
                        changeState(State.READ_FILE_NAME_LENGTH);
                        break;

                    case REFRESH_SERVER_FILE_AND_DIRECTORY_LIST:
                        changeState(State.READ_REMOTE_FILE_LIST_LENGTH);
                        break;

                    case AUTHORIZATION_OK:
                        callbackOnAuthorizationPass.call(null);
                        changeState(State.IDLE);
                        break;

                    case AUTHORIZATION_FAILED:
                        callbackOnAuthorizationFailed.call(null);
                        changeState(State.IDLE);
                        break;
                }
            }

            if (currentState.equals(State.READ_REMOTE_FILE_LIST_LENGTH) && buf.readableBytes() >= 8) {
                fileListLength = buf.readLong();
                fileList = new byte[(int) fileListLength];
                log.info("STATE: " + currentState + ". List length: " + fileListLength);

                changeState(State.READ_REMOTE_FILE_LIST);
            }

            if (currentState.equals(State.READ_REMOTE_FILE_LIST) && buf.readableBytes() > 0) {
                fileList[(int) redFileListLength] = buf.readByte();
                redFileListLength++;
            }

            if (currentState == State.READ_FILE_NAME_LENGTH && buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                log.info("State: " + currentState + " nextLength: " + nextLength);

                changeState(State.READ_FILE_NAME);
            }

            if (currentState == State.READ_FILE_NAME && buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                log.info("STATE: " + currentState + ". Filename received - " + new String(fileName, StandardCharsets.UTF_8));
                out = new BufferedOutputStream(Files.newOutputStream(Paths.get(currentDir + "/" + new String(fileName))));
                callbackOnReceivedFile.call(new ArrayList<>());
                changeState(State.READ_FILE_LENGTH);
            }


            if (currentState == State.READ_FILE_LENGTH && buf.readableBytes() >= 8) {
                fileLength = buf.readLong();
                log.info("STATE: " + currentState + ". File length - " + fileLength);

                changeState(State.READ_FILE);
            }


            if (currentState == State.READ_FILE && buf.readableBytes() > 0) {
                out.write(buf.readByte());
                redFileLength++;
            }
        }

        if (currentState == State.READ_FILE && fileLength == redFileLength) {
            log.info("STATE: " + currentState + ". File received");
            out.close();
            redFileLength = 0L;
            changeState(State.IDLE);
        }

        if (currentState == State.READ_REMOTE_FILE_LIST && fileListLength == redFileListLength) {
            log.info("STATE: " + currentState + ". File list received");
            redFileListLength = 0L;

            ByteArrayInputStream b = new ByteArrayInputStream(fileList);
            ObjectInputStream o = new ObjectInputStream(b);
            List<String> list = (List<String>) o.readObject();
            b.close();
            o.close();

            callbackOnReceivedFileList.call(list);

            log.info("STATE: " + currentState + ". Remote files list: " + list);
            changeState(State.IDLE);
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


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}

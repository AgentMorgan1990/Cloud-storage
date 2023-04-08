package org.example.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Commands;
import org.example.server.SentService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProtocolInboundHandler extends ChannelInboundHandlerAdapter {
    private enum State {
        IDLE,
        READ_PACKAGE_SIZE,
        READ_COMMAND,
        EXECUTE_COMMAND,
        READ_FILE_NAME_LENGTH,
        READ_FILE_NAME,
        READ_FILE_LENGTH,
        READ_FILE,
        READ_SEND_FILE_NAME_LENGTH,
        READ_SEND_FILE_NAME,
        GO_TO_PARENT_DIRECTORY,
        READ_DIRECTORY_NAME_LENGTH,
        READ_DIRECTORY_NAME,
        READ_DIRECTORY_NAME_LENGTH_FOR_DELETING,
        READ_DIRECTORY_NAME_FOR_DELETING,
        READ_DIRECTORY_NAME_LENGTH_FOR_CREATING,
        READ_DIRECTORY_NAME_FOR_CREATING
    }

    private State currentState = State.IDLE;
    private Commands command;
    private int nextLength;
    private long fileLength;
    private long redFileSize = 0L;
    private BufferedOutputStream out;
    private final SentService sentService;
    private Path rootDir;
    private Path currentDir;
    private static final Logger log = LogManager.getLogger(ProtocolInboundHandler.class);


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

                changeState(State.READ_COMMAND);
            }

            if (currentState.equals(State.READ_COMMAND) && buf.readableBytes() >= 4) {
                byte[] commandTitle = new byte[buf.readInt()];
                buf.readBytes(commandTitle);
                command = Commands.valueOf(new String(commandTitle, StandardCharsets.UTF_8));
                log.info("State: " + currentState + " command: " + command);

                changeState(State.EXECUTE_COMMAND);
            }


            if (currentState.equals(State.EXECUTE_COMMAND)) {

                switch (command) {
                    case SEND_FILE_TO_SERVER:
                        changeState(State.READ_FILE_NAME_LENGTH);
                        break;

                    case SEND_FILE_FROM_SERVER:
                        changeState(State.READ_SEND_FILE_NAME_LENGTH);
                        break;

                    case REFRESH_SERVER_FILE_AND_DIRECTORY_LIST:
                        sentService.sendFileList(currentDir, ctx, future -> {
                            if (!future.isSuccess()) {
                                future.cause().printStackTrace();
                            }
                            if (future.isSuccess()) {
                                changeState(State.IDLE);
                            }
                        });
                        break;

                    case GO_TO_SERVER_PARENT_DIRECTORY:
                        changeState(State.GO_TO_PARENT_DIRECTORY);
                        break;

                    case GO_TO_SERVER_DIRECTORY:
                        changeState(State.READ_DIRECTORY_NAME_LENGTH);
                        break;

                    case DELETE_FILE_OR_DIRECTORY_ON_SERVER:
                        changeState(State.READ_DIRECTORY_NAME_LENGTH_FOR_DELETING);
                        break;

                    case CREATE_DIRECTORY_ON_SERVER:
                        changeState(State.READ_DIRECTORY_NAME_LENGTH_FOR_CREATING);
                        break;
                }
            }

            if (currentState.equals(State.READ_DIRECTORY_NAME_LENGTH_FOR_CREATING) && buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                log.info("State: " + currentState + " directory name length: " + nextLength);

                changeState(State.READ_DIRECTORY_NAME_LENGTH_FOR_CREATING);
            }

            if (currentState.equals(State.READ_DIRECTORY_NAME_LENGTH_FOR_CREATING) && buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                String dirName = new String(fileName, StandardCharsets.UTF_8);

                File theDir = new File(currentDir + "/" + dirName);
                if (!theDir.exists()) theDir.mkdirs();

                log.info("State: " + currentState + " directory name: " + dirName);

                sentService.sendFileList(currentDir, ctx, future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        changeState(State.IDLE);
                    }
                });
            }

            if (currentState.equals(State.READ_DIRECTORY_NAME_LENGTH_FOR_DELETING) && buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                log.info("State: " + currentState + " directory name length: " + nextLength);

                changeState(State.READ_DIRECTORY_NAME_LENGTH_FOR_DELETING);
            }

            if (currentState.equals(State.READ_DIRECTORY_NAME_LENGTH_FOR_DELETING) && buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                String dirName = new String(fileName, StandardCharsets.UTF_8);

                Files.delete(Paths.get(currentDir + "/" + dirName));
                log.info("State: " + currentState + " directory name: " + dirName);

                sentService.sendFileList(currentDir, ctx, future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        changeState(State.IDLE);
                    }
                });
            }


            if (currentState.equals(State.READ_DIRECTORY_NAME_LENGTH) && buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                log.info("State: " + currentState + " directory name length: " + nextLength);

                changeState(State.READ_DIRECTORY_NAME);
            }

            if (currentState.equals(State.READ_DIRECTORY_NAME) && buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                String dirName = new String(fileName, StandardCharsets.UTF_8);
                currentDir = Paths.get(currentDir.toString() + "/" + dirName);
                log.info("State: " + currentState + " directory name: " + currentDir.toString());
                sentService.sendFileList(currentDir, ctx, future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        changeState(State.IDLE);
                    }
                });
            }

            if (currentState.equals(State.GO_TO_PARENT_DIRECTORY)) {

                if (!rootDir.equals(currentDir)){
                    currentDir = currentDir.getParent();
                }

                log.info("State: " + currentState + " directory name: " + currentDir.toString());
                sentService.sendFileList(currentDir, ctx, future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        changeState(State.IDLE);
                    }
                });
            }


            if (currentState.equals(State.READ_SEND_FILE_NAME_LENGTH) && buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                log.info("State: " + currentState + " nextLength: " + nextLength);

                changeState(State.READ_SEND_FILE_NAME);
            }


            if (currentState.equals(State.READ_SEND_FILE_NAME) && buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                String fileName1 = new String(fileName, StandardCharsets.UTF_8);
                log.info("State: " + currentState + " nextLength: " + nextLength);
                sentService.sendFile(currentDir, fileName1, ctx, future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        changeState(State.IDLE);
                    }
                });
            }


            if (currentState.equals(State.READ_FILE_NAME_LENGTH) && buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                log.info("State: " + currentState + " nextLength: " + nextLength);

                changeState(State.READ_FILE_NAME);
            }

            if (currentState.equals(State.READ_FILE_NAME) && buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                log.info("State: " + currentState + " Filename received: " + new String(fileName, StandardCharsets.UTF_8));

                out = new BufferedOutputStream(Files.newOutputStream(Paths.get(currentDir + "/" + new String(fileName))));

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
            sentService.sendFileList(currentDir, ctx, future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                }
                if (future.isSuccess()) {
                    changeState(State.IDLE);
                }
            });
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

package com.geekbrains.netty;

import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.geekbrains.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileMessageHandler extends SimpleChannelInboundHandler<Command> {

    private static Path currentPath;
    private static Path clientPath;
    DBAuthService service = DBAuthService.getInstance();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("Client connected!");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command cmd) throws Exception {
        log.debug("Received command from client: {}", cmd.getType());
        switch (cmd.getType()) {
            case FILE_MESSAGE:
                FileMessage fileMessage = (FileMessage) cmd;
                Files.write(
                        currentPath.resolve(fileMessage.getName()),
                        fileMessage.getBytes()
                );
                ctx.writeAndFlush(new ListResponse(currentPath));
                log.debug("Received a file {} from the client", fileMessage.getName());
                break;

            case FILE_REQUEST:
                FileRequest fileRequest = (FileRequest) cmd;
                String fileName = fileRequest.getName();
                Path file = Paths.get(String.valueOf(currentPath), fileName);
                ctx.writeAndFlush(new FileMessage(file));
                log.debug("Send file {} to the client", fileName);
                break;

            case LIST_REQUEST:
                ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                ctx.writeAndFlush(new ListResponse(currentPath));
                log.debug("Send list of files to the client");
                break;

            case PATH_UP_REQUEST:
                if (currentPath.getParent() != null) {
                    if (currentPath.equals(clientPath)) {
                        log.debug("Above the client's folder , it is not necessary to rise");
                    } else {
                        currentPath = currentPath.getParent();
                    }
                }
                log.debug("Send list of files and current directory to the client");
                ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                ctx.writeAndFlush(new ListResponse(currentPath));
                break;

            case PATH_IN_REQUEST:
                PathInRequest request = (PathInRequest) cmd;
                Path newPAth = currentPath.resolve(request.getDir());
                if (Files.isDirectory(newPAth)) {
                    currentPath = newPAth;
                    log.debug("Send list of files and current directory to the client");
                    ctx.writeAndFlush(new PathResponse(currentPath.toString()));
                    ctx.writeAndFlush(new ListResponse(currentPath));
                } else {
                    log.debug("{} is not a directory",request);
                }
            case AUTH_REQUEST:
                AuthRequest authRequest = (AuthRequest) cmd;
                String login = authRequest.getLogin();
                String password = authRequest.getPassword();
                AuthResponse authResponse = new AuthResponse();
                try {
                if (service.findByLogin(login).equals(password)) {
                    authResponse.setAuthStatus(true);
                    clientPath = Paths.get("./", login);
//                    clientPath = Paths.get("D:\\GB cloud storage\\Lesson_1\\cloud-storage-sep-2021\\server-sep-2021", login);
                    if (!Files.exists(clientPath)) {
                        Files.createDirectory(clientPath);
                    }
                    currentPath = clientPath;
                } else {
                    authResponse.setAuthStatus(false);
                }} catch (Exception e){
                    authResponse.setAuthStatus(false);
                }
                ctx.writeAndFlush(authResponse);
                break;
            default:
                log.debug("Invalid command {}", cmd.getType());
                break;
        }
    }
}

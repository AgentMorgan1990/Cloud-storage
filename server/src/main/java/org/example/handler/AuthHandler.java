package org.example.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Command;
import org.example.server.AuthorizationService;
import org.example.server.SentService;

import java.nio.charset.StandardCharsets;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private enum State {
        IDLE,
        READ_PACKAGE_SIZE,
        READ_COMMAND,
        EXECUTE_COMMAND,
        READ_LOGIN_LENGTH,
        READ_LOGIN,
        READ_PASSWORD_LENGTH,
        READ_PASSWORD
    }

    private State currentState = State.IDLE;
    private int nextLength;
    private Command command;
    private String login;
    private AuthorizationService authorizationService = new AuthorizationService();
    private SentService sentService = new SentService();

    private static final Logger log = LogManager.getLogger(AuthHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

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
                command = Command.valueOf(new String(commandTitle, StandardCharsets.UTF_8));
                log.info("State: " + currentState + " command: " + command);

                changeState(State.EXECUTE_COMMAND);
            }


            if (currentState.equals(State.EXECUTE_COMMAND)) {

                switch (command) {
                    case AUTHORIZATION_REQUEST:
                        changeState(State.READ_LOGIN_LENGTH);
                        break;
                }
            }

            if (currentState.equals(State.READ_LOGIN_LENGTH) && buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                log.info("State: " + currentState + " login name length: " + nextLength);

                changeState(State.READ_LOGIN);
            }

            if (currentState.equals(State.READ_LOGIN) && buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                login = new String(fileName, StandardCharsets.UTF_8);

                log.info("State: " + currentState + " login: " + login);

                changeState(State.READ_PASSWORD_LENGTH);
            }

            if (currentState.equals(State.READ_PASSWORD_LENGTH) && buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                log.info("State: " + currentState + " password length: " + nextLength);

                changeState(State.READ_PASSWORD);
            }

            if (currentState.equals(State.READ_PASSWORD) && buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                String password = new String(fileName, StandardCharsets.UTF_8);

                log.info("State: " + currentState + " password: " + password.length());

                if (authorizationService.check(login, password)) {
                    sentService.sendCommand(ctx, Command.AUTHORIZATION_OK);
                    ctx.pipeline().addLast(new ProtocolInboundHandler(login, ctx));
                    log.info("State: " + currentState + " authorization pass");
                } else {
                    sentService.sendCommand(ctx, Command.AUTHORIZATION_FAILED);
                    log.info("State: " + currentState + " authorization failed");
                }
                changeState(State.IDLE);
            }
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
}

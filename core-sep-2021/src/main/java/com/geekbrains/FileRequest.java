package com.geekbrains;

import java.nio.file.Path;

//Класс-команда для запроса отправки файла с сервера на клиент
public class FileRequest extends Command {
    private final String name;

    public FileRequest(Path path) {
        name = path.getFileName().toString();
    }

    public String getName() {
        return name;
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_REQUEST;
    }
}

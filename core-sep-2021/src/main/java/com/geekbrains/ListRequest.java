package com.geekbrains;

//Класс-команда для запроса у сервера передачи списка файлов и папок в текущей директории

public class ListRequest extends Command{
    @Override
    public CommandType getType() {
        return CommandType.LIST_REQUEST;
    }
}

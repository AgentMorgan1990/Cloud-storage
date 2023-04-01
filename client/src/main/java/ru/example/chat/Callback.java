package ru.example.chat;
import java.util.List;

@FunctionalInterface
public interface Callback {

    //todo можно пихнуть сюда абстрактный класс, которыя наполнять разными командами,
    // а в зависимости от окмманд разное наполнение объекта
    void call(List<String> fileList);
}


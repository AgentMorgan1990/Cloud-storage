package ru.example.chat;

import java.io.IOException;
import java.util.List;



    public interface Callback {
        void call(List<String> fileList) throws IOException, InterruptedException;
    }


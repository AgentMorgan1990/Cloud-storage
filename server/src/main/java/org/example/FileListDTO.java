package org.example;

import org.example.model.Commands;

import java.util.List;

public class FileListDTO {

    Commands command;
    List<String> fileList;

    public FileListDTO(Commands command, List<String> fileList) {
        this.command = command;
        this.fileList = fileList;
    }

    public Commands getCommand() {
        return command;
    }

    public List<String> getFileList() {
        return fileList;
    }
}

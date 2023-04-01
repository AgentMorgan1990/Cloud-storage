package org.example.server;

import io.netty.channel.ChannelHandlerContext;
import org.example.FileListDTO;
import org.example.model.Commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class SentService {



    public void sentServerFileList(ChannelHandlerContext ctx){

        List<String> filesList = null;
        try {
            filesList =  Files.list(Paths.get("server_storage"))
                    .sorted((o1, o2) -> {
                        if (Files.isDirectory(o1) && !Files.isDirectory(o2)) {
                            return -1;
                        } else if (!Files.isDirectory(o1) && Files.isDirectory(o2)) {
                            return 1;
                        } else return 0;
                    })
                    .map(p -> {
                        if (!Files.isDirectory(p)) {
                            return p.getFileName().toString();
                        } else {
                            return "[dir]" + p.getFileName().toString();
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileListDTO dto = new FileListDTO(Commands.SENT_FILE_LIST,filesList);



    }

}

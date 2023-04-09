package org.example.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static byte[] getCurrentDirectoryContent(Path currentDir) {

        List<String> filesList = null;
        try {
            filesList = Files.list(Paths.get(currentDir.toString()))
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
                            return "[dir] " + p.getFileName().toString();
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String str: filesList) {
            System.out.println(str);
        }


        try (
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                ObjectOutputStream o = new ObjectOutputStream(b);
        ) {
            o.writeObject(filesList);
            return b.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}

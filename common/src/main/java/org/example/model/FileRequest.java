package org.example.model;

public class FileRequest extends AbstractMessage {

    private static final long serialVersionUID = 5193392663743561684L;
    private String filename;

    public String getFilename() {
        return filename;
    }

    public FileRequest(String filename) {
        this.filename = filename;
    }
}

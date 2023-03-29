package ru.example.chat;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.SneakyThrows;


import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
public class MainController implements Initializable {
    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> filesList;

    private ProtoFileSender protoFileSender;

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        protoFileSender = new ProtoFileSender();
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        networkStarter.await();

    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) throws IOException {

            try {
                protoFileSender.downFile(Paths.get("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/VideoTest.mkv"), Network.getInstance().getCurrentChannel(), future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        System.out.println("Файл успешно передан");
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    public void pressOnUploadBtn(ActionEvent actionEvent) {

            protoFileSender.uploadFile(Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Файл успешно передан");
            }
        });
    }

    public void pressOnAutentificationBtn (ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,"Тут пока заглушка ***", ButtonType.CLOSE);
        alert.showAndWait();
    }

    public void refreshLocalFilesList() {
        Platform.runLater(() -> {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get("client_storage"))
                        .filter(p -> !Files.isDirectory(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

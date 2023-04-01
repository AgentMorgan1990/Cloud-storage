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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
public class MainController implements Initializable {


    @FXML
    TextField fileField;
    @FXML
    ListView<String> clientFilesList, serverFilesList;

    private ProtoFileSender protoFileSender;
    private static Path currentDir = Paths.get("/");


//    private Callback callback;

    private Network network;

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        network = Network.getInstance();
        addNavigationListener();
        protoFileSender = new ProtoFileSender();
        CountDownLatch networkStarter = new CountDownLatch(1);
        //todo надо будет навешать колбэки на методы, а не в инициализации
        new Thread(() -> network.start(networkStarter, callback -> {
            callback.forEach(o -> serverFilesList.getItems().add(o));
        })).start();
        networkStarter.await();
        refreshLocalFilesList();
        refreshRemoteFilesList();
    }


    public void addNavigationListener() {
        clientFilesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                fileField.clear();
                fileField.appendText(clientFilesList.getSelectionModel().getSelectedItem());
            }
        });
        serverFilesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                fileField.clear();
                fileField.appendText(serverFilesList.getSelectionModel().getSelectedItem());
            }
        });
    }
    public void pressOnDownloadBtn(ActionEvent actionEvent) throws IOException {
            try {
                //todo тут нужно поработать с относительными и абсолютными путями, ограничить доступы программы к хранилищам
                protoFileSender.downFile(Paths.get("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/client_storage/" + fileField.getText()),
                        network.getCurrentChannel(), future -> {
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

        String fileName = fileField.getText();

        protoFileSender.uploadFile(fileName, network.getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Файл успешно запрошен "+ fileName);
            }
        });
    }

    public void pressOnAutentificationBtn (ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,"Тут пока заглушка ***", ButtonType.CLOSE);
        alert.showAndWait();
    }


    public void refreshRemoteFilesList(){
        protoFileSender.refreshRemoteFileList(network.getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Файл успешно передан");
            }
        });
    }

    public void refreshLocalFilesList() {
        clientFilesList.getItems().clear();
        Platform.runLater(() -> {
            try {
                Files.list(Paths.get("client_storage"))
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
                                return "[" + p.getFileName().toString() + "]";
                            }
                        })
                        .forEach(o -> clientFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

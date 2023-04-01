package ru.example.chat;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.SneakyThrows;

import java.io.File;
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
    private Path rootDir = Paths.get("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/client_storage/");
    private Path currentDir = Paths.get("/");

    private Network network;

    //todo вынести идентификатор [dir] в финал переменную

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        currentDir = rootDir;

        Image packageImage  = new Image(
                Files.newInputStream(Paths.get("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/client/src/main/resources/image_2.png")),
                15.0,
                15.0,
                false,
                false);

        Image fileImage  = new Image(
                Files.newInputStream(Paths.get("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/client/src/main/resources/image_3.png")),
                15.0,
                15.0,
                false,
                true);

        clientFilesList.setCellFactory(param -> new ListCell<String>() {
            private ImageView imageView = new ImageView();
            @Override
            public void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty ) {
                    setText(null);
                    setGraphic(null);
                } else if (name.startsWith("[dir]")){
                    imageView.setImage(packageImage);
                    setText(name.substring(6));
                    setGraphic(imageView);
                } else if (name.equals("/..")){
                    setText(name);
                    setGraphic(null);
                } else {
                    imageView.setImage(fileImage);
                    setText(name);
                    setGraphic(imageView);
                }
            }
        });

        serverFilesList.setCellFactory(param -> new ListCell<String>() {
            private final ImageView imageView = new ImageView();
            @Override
            public void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (name.startsWith("[dir]")){
                    imageView.setImage(packageImage);
                    setText(name.substring(6));
                    setGraphic(imageView);
                } else if (name.equals("/..")){
                    setText(name);
                    setGraphic(null);
                } else {
                    imageView.setImage(fileImage);
                    setText(name);
                    setGraphic(imageView);
                }
            }
        });



        network = Network.getInstance();
        addNavigationListener();
        protoFileSender = new ProtoFileSender();
        CountDownLatch networkStarter = new CountDownLatch(1);
        //todo надо будет навешать колбэки на методы, а не в инициализации
        new Thread(() -> network.start(networkStarter, callback -> {
            serverFilesList.getItems().clear();
            serverFilesList.getItems().add("/..");
            callback.forEach(o -> serverFilesList.getItems().add(o));
        })).start();
        networkStarter.await();
        refreshLocalFilesList();
        refreshRemoteFilesList();
    }


    public void addNavigationListener() {
        clientFilesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String item = clientFilesList.getSelectionModel().getSelectedItem();
                if (item.equals("/..") && !currentDir.equals(rootDir)) {
                    currentDir = currentDir.getParent();
                    refreshLocalFilesList();
                    //todo поднять папку на уровень выше
                } else if (item.startsWith("[dir]")) {
                    currentDir = Paths.get(currentDir+"/"+item.substring(6));
                    refreshLocalFilesList();
                } else {
                    fileField.clear();
                    fileField.appendText(clientFilesList.getSelectionModel().getSelectedItem());
                }
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

    public void pressOnCreateLocalBtn(ActionEvent actionEvent) {
        File theDir = new File(currentDir + "/" + fileField.getText());
        if (!theDir.exists()) {
            theDir.mkdirs();
        }
        refreshLocalFilesList();
    }

    public void pressOnDeleteLocalBtn(ActionEvent actionEvent) {
        String deletedName = clientFilesList.getSelectionModel().getSelectedItem();
        if (deletedName.startsWith("[dir]")) deletedName = deletedName.substring(6);
        try {
            Files.delete(Paths.get(currentDir + "/" + deletedName));
        } catch (Exception e) {
            e.printStackTrace();
        }
        refreshLocalFilesList();
    }


    private void refreshRemoteFilesList(){
        protoFileSender.refreshRemoteFileList(network.getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Файл успешно передан");
            }
        });
    }

    private void refreshLocalFilesList() {
        Platform.runLater(() -> {
            clientFilesList.getItems().clear();
            clientFilesList.getItems().add("/..");
            try {
                Files.list(Paths.get(currentDir.toString()))
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
                        }).forEach(o -> clientFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

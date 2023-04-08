package ru.example.chat;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.model.Command;
import org.example.model.SentService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class MainController implements Initializable {

    @FXML
    TextField fileField, loginField, passwordField;
    @FXML
    ListView<String> clientFilesList, serverFilesList;
    @FXML
    HBox authPanel;
    @FXML
    VBox mainPanel;

    private SentService sentService;
    private Path rootDir = Paths.get("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/client_storage/");
    private Path currentDir = Paths.get("/");

    private Network network;
    private static final Logger log = LogManager.getLogger(MainController.class);

    //todo вынести идентификатор [dir] в финал переменную

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        currentDir = rootDir;

        Image packageImage = new Image(
                Files.newInputStream(Paths.get("/home/sergei/IdeaProjects/Educational projects/Cloud-storage/client/src/main/resources/image_2.png")),
                15.0,
                15.0,
                false,
                false);

        Image fileImage = new Image(
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
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (name.startsWith("[dir]")) {
                    imageView.setImage(packageImage);
                    setText(name.substring(6));
                    setGraphic(imageView);
                } else if (name.equals("/..")) {
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
                } else if (name.startsWith("[dir]")) {
                    imageView.setImage(packageImage);
                    setText(name.substring(6));
                    setGraphic(imageView);
                } else if (name.equals("/..")) {
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
        sentService = new SentService();
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> network.start(networkStarter, currentDir)).start();
        networkStarter.await();
        updateCurrentDir(currentDir);

        Network.getInstance().setOnReceivedFileCallback((callback) -> {
            Platform.runLater(() -> {
                refreshLocalFilesList();
            });
        });

        Network.getInstance().setOnReceivedFileListCallback((callback) -> {
            Platform.runLater(() -> {
                serverFilesList.getItems().clear();
                serverFilesList.getItems().add("/..");
                callback.forEach(o -> serverFilesList.getItems().add(o));

            });
        });

        Network.getInstance().setOnAuthorizationPass((callback) -> {
            authPanel.setVisible(false);
            mainPanel.setVisible(true);
            refreshLocalFilesList();
            refreshRemoteFilesList();
        });

        Network.getInstance().setOnAuthorizationFailed((callback) -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Неверный логин или пароль");
                alert.showAndWait();
            });
        });
    }


    public void addNavigationListener() {
        clientFilesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String item = clientFilesList.getSelectionModel().getSelectedItem();
                if (item.equals("/..") && !currentDir.equals(rootDir)) {
                    updateCurrentDir(currentDir = currentDir.getParent());
                    refreshLocalFilesList();
                } else if (item.startsWith("[dir]")) {
                    updateCurrentDir(currentDir = Paths.get(currentDir + "/" + item.substring(6)));
                    refreshLocalFilesList();
                } else {
                    fileField.clear();
                    fileField.appendText(item);
                }
            }
        });
        serverFilesList.setOnMouseClicked(event -> {
            String item = serverFilesList.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 2) {
                if (item.equals("/..")) {
                    sentService.sendMessage(
                            network.getCurrentChannel(),
                            Command.GO_TO_SERVER_PARENT_DIRECTORY,
                            null,
                            future -> {
                                if (!future.isSuccess()) {
                                    future.cause().printStackTrace();
                                }
                                if (future.isSuccess()) {
                                    log.info("Send request to go to remote parent directory");
                                }
                            });
                } else if (item.startsWith("[dir]")) {
                    sentService.sendMessage(
                           network.getCurrentChannel(),
                            Command.GO_TO_SERVER_DIRECTORY,
                            item.substring(6).getBytes(),
                            future -> {
                                if (!future.isSuccess()) {
                                    future.cause().printStackTrace();
                                }
                                if (future.isSuccess()) {
                                    log.info("Send request to go to remote directory: " + item);
                                }
                            });
                } else {
                    fileField.clear();
                    fileField.appendText(item);
                }
            }
        });
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        sentService.sendFile(
                Command.SEND_FILE_TO_SERVER,
                currentDir,
                fileField.getText(),
                network.getCurrentChannel(),
                future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        log.info("File was successfully transferred");
                    }
                }
        );
    }

    public void pressOnUploadBtn(ActionEvent actionEvent) {
        String fileName = fileField.getText();
        sentService.sendMessage(
                network.getCurrentChannel(),
                Command.SEND_FILE_FROM_SERVER,
                fileName.getBytes(),
                future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        log.info("File " + fileName + " successfully requested");
                    }
                }
        );
    }

    public void pressOnAuthorizationBtn(ActionEvent actionEvent) {

        String loginAndPassword = loginField.getText() + " " + passwordField.getText();
        log.error(loginAndPassword);
        sentService.sendMessage(
                network.getCurrentChannel(),
                Command.AUTHORIZATION_REQUEST,
                loginAndPassword.getBytes(),
                future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        log.info("Request to authorization has been sent to server");
                    }
                }
        );
        loginField.clear();
        passwordField.clear();
    }

    public void pressOnCreateLocalBtn(ActionEvent actionEvent) {

        TextInputDialog dialog = new TextInputDialog("new_directory");
        dialog.setTitle("Create new local directory");
        dialog.setHeaderText("Look, a Text Input Dialog");
        dialog.setContentText("Please enter directory name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            File theDir = new File(currentDir + "/" + result.get());
            if (!theDir.exists()) {
                theDir.mkdirs();
            }
            refreshLocalFilesList();

        }
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

    public void pressOnCreateRemoteBtn(ActionEvent actionEvent) {

        TextInputDialog dialog = new TextInputDialog("new_directory");
        dialog.setTitle("Create new remote directory");
        dialog.setHeaderText("Look, a Text Input Dialog");
        dialog.setContentText("Please enter directory name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            sentService.sendMessage(
                    network.getCurrentChannel(),
                    Command.CREATE_DIRECTORY_ON_SERVER,
                    result.get().getBytes(),
                    future -> {
                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            log.info("Request to creating directory has been sent to server");
                        }
                    });
            refreshLocalFilesList();
        }
    }

    public void pressOnDeleteRemoteBtn(ActionEvent actionEvent) {
        String deletedName = serverFilesList.getSelectionModel().getSelectedItem();
        if (deletedName.startsWith("[dir]")) deletedName = deletedName.substring(6);

        sentService.sendMessage(
                network.getCurrentChannel(),
                Command.DELETE_FILE_OR_DIRECTORY_ON_SERVER,
                deletedName.getBytes(),
                future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        log.info("Request to deleting file or directory has been sent to server");
                    }
                }
        );
    }


    private void refreshRemoteFilesList() {
        sentService.sendMessage(
                network.getCurrentChannel(),
                Command.REFRESH_SERVER_FILE_AND_DIRECTORY_LIST,
                null,
                future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        log.info("Request to get remote file list has been sent to server");
                    }
                }

        );
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

    private void updateCurrentDir(Path currentDir){
        Network.getInstance().setCurrentDir(currentDir);
    }
}

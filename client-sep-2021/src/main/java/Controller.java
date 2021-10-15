
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.geekbrains.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Controller implements Initializable {

//    private static Path currentDir = Paths.get("client-sep-2021", "root");
    private static Path currentDir = Paths.get("D:\\GB cloud storage\\Lesson_1\\cloud-storage-sep-2021\\client-sep-2021", "root");
    public ListView<String> fileClientView;
    public ListView<String> fileServerView;
    public TextField input;
    public TextField currentDirectoryOnClient;
    public TextField currentDirectoryOnServer;
    public AnchorPane mainScene;

    public TextField loginField;
    public TextField passwordField;
    public Button Authorization;

    private Net net;

    public void sendLoginAndPassword(ActionEvent actionEvent) {
        String login = loginField.getText();
        String password = passwordField.getText();
        loginField.clear();
        passwordField.clear();
        net.sendCommand(new AuthRequest(login, password));
    }

    public void sendFile(ActionEvent actionEvent) throws IOException {
        String fileName = input.getText();
        input.clear();
        Path file = Paths.get(String.valueOf(currentDir.resolve(fileName)));
        net.sendCommand(new FileMessage(file));
    }

    public void receiveArrayFiles(ActionEvent actionEvent) {
        net.sendCommand(new ListRequest());
    }

    public void updateArrayFiles(ActionEvent actionEvent) throws IOException {
        refreshClientView();
    }

    public void receiveFile(ActionEvent actionEvent) {
        String fileName = input.getText();
        input.clear();
        Path file = Paths.get(fileName);
        net.sendCommand(new FileRequest(file));
    }

    public void clientPathUp(ActionEvent actionEvent) throws IOException {
        currentDir = currentDir.getParent();
        currentDirectoryOnClient.setText(currentDir.toString());
        refreshClientView();
    }

    public void serverPathUp(ActionEvent actionEvent) {
        net.sendCommand(new PathUpRequest());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            currentDirectoryOnClient.setText(currentDir.toString());
            refreshClientView();
            addNavigationListener();
        } catch (IOException e) {
            e.printStackTrace();
        }
        net = Net.getInstance(cmd -> {
                    switch (cmd.getType()) {
                        case LIST_RESPONSE:
                            ListResponse listResponse = (ListResponse) cmd;
                            Platform.runLater(() -> refreshServerView(listResponse.getList()));
                            break;
                        case FILE_MESSAGE:
                            FileMessage fileMessage = (FileMessage) cmd;
                            Files.write(
                                    currentDir.resolve(fileMessage.getName()),
                                    fileMessage.getBytes()
                            );
                            Platform.runLater(() -> {
                                try {
                                    refreshClientView();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            break;
                        case PATH_RESPONSE:
                            PathResponse pathResponse = (PathResponse) cmd;
                            currentDirectoryOnServer.setText(pathResponse.getPath());
                            break;
                        case AUTH_RESPONSE:
                            AuthResponse authResponse = (AuthResponse) cmd;
                            log.debug("AuthResponse {}", authResponse.getAuthStatus());
                            if (authResponse.getAuthStatus()) {
                                mainScene.setVisible(true);
                                loginField.setVisible(false);
                                passwordField.setVisible(false);
                                Authorization.setVisible(false);
                                net.sendCommand(new ListRequest());
                            } else if(!authResponse.getAuthStatus()){
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.WARNING, "Неверный логин или пароль",
                                            ButtonType.OK);
                                    alert.showAndWait();
                                });
                            }

                            break;
                        default:
                            log.debug("Invalid command {}", cmd.getType());
                    }
                }
        );
    }
    public String resolveFileType(Path path) {
        if (Files.isDirectory(path)) {
            return "[Dir]" + " " + path.getFileName().toString();
        } else {
            return "[File]" + " " + path.getFileName().toString();
        }
    }

    public String returnName2(String str) {
        String[] words = str.split(" ");
        String returnWay;
        switch (words.length) {
            case 2:
                returnWay = words[1];
                break;
            case 3:
                returnWay = words[1] + " " + words[2];
                break;
            case 4:
                returnWay = words[1] + " " + words[2] + " " + words[3];
                break;
            default:
                returnWay = words[1];
                break;
                //todo поменять на решение с циклом
//        String returnWay = words[1];
//        if (words.length > 2) {
//            for (int i = 3; i < (words.length + 1); i++) {
//                returnWay = returnWay + " " + words[i];
//            }
//            System.out.println(returnWay);
//        }

        }
        return returnWay;
    }

    public String returnName1(String str) {
        String[] words = str.split(" ");
        return words[0];
    }

    public void refreshServerView(List<String> names) {
        fileServerView.getItems().clear();
        fileServerView.getItems().addAll(names);
    }


    private void refreshClientView() throws IOException {
        fileClientView.getItems().clear();
        List<String> names = Files.list(currentDir)
                .map(this::resolveFileType)
                .collect(Collectors.toList());
        fileClientView.getItems().addAll(names);
    }

    public void addNavigationListener() {
        fileClientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = returnName2(fileClientView.getSelectionModel().getSelectedItem());
                Path newPath = currentDir.resolve(item);
                if (Files.isDirectory(newPath)) {
                    currentDir = newPath;

                    try {
                        refreshClientView();
                        currentDirectoryOnClient.setText(currentDir.toString());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } else {
                    input.setText(item);
                }
            }
        });
        fileServerView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = returnName2(fileServerView.getSelectionModel().getSelectedItem());
                if (returnName1(fileServerView.getSelectionModel().getSelectedItem()).equals("[Dir]")) {
                    net.sendCommand(new PathInRequest(item));
                } else {
                    input.setText(item);
                }
            }
        });
    }
}

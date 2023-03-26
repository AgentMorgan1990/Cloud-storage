module ru.example.cloudstorage.client {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.example.cloudstorage.client to javafx.fxml;
    exports ru.example.cloudstorage.client;
}
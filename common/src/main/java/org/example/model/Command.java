package org.example.model;

public enum Command {
    SEND_FILE_FROM_SERVER,
    SEND_FILE_TO_SERVER,
    REFRESH_SERVER_FILE_AND_DIRECTORY_LIST,
    GO_TO_SERVER_DIRECTORY,
    GO_TO_SERVER_PARENT_DIRECTORY,
    DELETE_FILE_OR_DIRECTORY_ON_SERVER,
    CREATE_DIRECTORY_ON_SERVER,
    AUTHORIZATION_REQUEST,
    AUTHORIZATION_OK,
    AUTHORIZATION_FAILED
}

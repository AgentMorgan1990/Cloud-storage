package com.geekbrains.netty;

    public interface AuthService<T> extends CrudService<T, Long> {
        String findByLogin(String login);
    }

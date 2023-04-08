package org.example.server;


import java.util.HashMap;
import java.util.Map;

public class AuthorizationService {


    private Map<String, String> clients = new HashMap<String, String>() {{
        put("Bob", "100");
        put("Alex", "200");
    }};


    public boolean check(String login, String password) {

        return clients.containsKey(login) && clients.get(login).equals(password);

    }
}

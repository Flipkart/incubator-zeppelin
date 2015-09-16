package org.apache.zeppelin.utils;

import java.io.Serializable;

/**
 * Created by piyush.mukati on 14/09/15.
 */
public class SecurityContext implements Serializable {
    private String user;
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
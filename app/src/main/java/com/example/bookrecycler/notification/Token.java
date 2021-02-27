package com.example.bookrecycler.notification;

//Token is the id of the device in Firebase Cloud Messaging
public class Token {
    String token;

    public Token(String token) {
        this.token = token;
    }

    public Token(){

    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

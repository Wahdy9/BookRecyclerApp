package com.example.bookrecycler.notification;

//Token is an auto id generated for each device in Firebase Cloud Messaging
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

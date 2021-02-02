package com.example.bookrecycler;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class MessageModel {

    private String sender;
    private String receiver;
    private String imageUrl;
    private boolean isMap;
    private boolean isImage;
    private String message;
    private Date timestamp;
    private GeoPoint geoPoint;

    public MessageModel() {
    }

    public MessageModel(String sender, String receiver, String imageUrl, boolean isMap, boolean isImage, String message, Date timestamp, GeoPoint geoPoint) {
        this.sender = sender;
        this.receiver = receiver;
        this.imageUrl = imageUrl;
        this.isMap = isMap;
        this.isImage = isImage;
        this.message = message;
        this.timestamp = timestamp;
        this.geoPoint = geoPoint;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isMap() {
        return isMap;
    }

    public void setMap(boolean map) {
        isMap = map;
    }

    public boolean isImage() {
        return isImage;
    }

    public void setImage(boolean image) {
        isImage = image;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }
}

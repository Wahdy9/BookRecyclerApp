package com.example.bookrecycler;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class MessageModel {

    private String id;
    private String sender;
    private String receiver;
    private String imageUrl;
    private boolean map;
    private boolean image;
    private String message;
    private Date timestamp;
    private GeoPoint geoPoint;

    public MessageModel() {
    }

    public MessageModel(String id,String sender, String receiver, String imageUrl, boolean map, boolean image, String message, Date timestamp, GeoPoint geoPoint) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.imageUrl = imageUrl;
        this.map = map;
        this.image = image;
        this.message = message;
        this.timestamp = timestamp;
        this.geoPoint = geoPoint;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        return map;
    }

    public void setMap(boolean map) {
        this.map = map;
    }

    public boolean isImage() {
        return image;
    }

    public void setImage(boolean image) {
        this.image = image;
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

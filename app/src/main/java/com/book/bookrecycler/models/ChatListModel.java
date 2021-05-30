package com.book.bookrecycler.models;

import java.util.Date;

public class ChatListModel {

    private String id;
    private boolean newMsgs;
    private Date timestamp;

    public ChatListModel() {
    }

    public ChatListModel(String id, boolean newMsgs, Date timestamp) {
        this.id = id;
        this.newMsgs = newMsgs;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isNewMsgs() {
        return newMsgs;
    }

    public void setNewMsgs(boolean newMsgs) {
        this.newMsgs = newMsgs;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}

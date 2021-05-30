package com.book.bookrecycler.models;

import com.google.firebase.Timestamp;

public class CommentModel {


    private String user_id,commentId,text, commentItemId;
    private Timestamp timestamp;


    public CommentModel() {
    }

    public CommentModel(String user_id, String commentId, String text, Timestamp timestamp, String commentItemId) {
        this.user_id = user_id;
        this.commentId = commentId;
        this.text = text;
        this.timestamp = timestamp;
        this.commentItemId = commentItemId;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getCommentItemId() {
        return commentItemId;
    }

    public void setCommentItemId(String commentItemId) {
        this.commentItemId = commentItemId;
    }
}

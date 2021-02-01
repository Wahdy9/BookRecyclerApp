package com.example.bookrecycler;

import java.io.Serializable;
import java.util.Date;

public class ItemModel implements Serializable {

    String itemId, userId, title, price, desc, category, condition, itemImg;
    Date timePosted;

    public ItemModel() {
    }

    public ItemModel(String itemId, String userId, String title, String price, String desc, String category, String condition, String itemImg, Date timePosted) {
        this.itemId = itemId;
        this.userId = userId;
        this.title = title;
        this.price = price;
        this.desc = desc;
        this.category = category;
        this.condition = condition;
        this.itemImg = itemImg;
        this.timePosted = timePosted;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getItemImg() {
        return itemImg;
    }

    public void setItemImg(String itemImg) {
        this.itemImg = itemImg;
    }

    public Date getTimePosted() {
        return timePosted;
    }

    public void setTimePosted(Date timePosted) {
        this.timePosted = timePosted;
    }
}

package com.book.bookrecycler.models;

public class UserModel {

    private String id;
    private String name;
    private String img_url;
    private String email;
    private String phone;
    private String province;
    private String country;
    private boolean showEmail;
    private boolean showPhone;

    public UserModel() {
    }

    public UserModel(String id, String name, String img_url, String email, String phone, String province, String country, boolean showEmail, boolean showPhone) {
        this.id = id;
        this.name = name;
        this.img_url = img_url;
        this.email = email;
        this.phone = phone;
        this.province = province;
        this.country = country;
        this.showEmail = showEmail;
        this.showPhone = showPhone;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImg_url() {
        return img_url;
    }

    public void setImg_url(String img_url) {
        this.img_url = img_url;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isShowEmail() {
        return showEmail;
    }

    public void setShowEmail(boolean showEmail) {
        this.showEmail = showEmail;
    }

    public boolean isShowPhone() {
        return showPhone;
    }

    public void setShowPhone(boolean showPhone) {
        this.showPhone = showPhone;
    }
}

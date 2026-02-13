package com.example.giftbook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "activity")
public class Activity {
    @Id
    private Long id;
    private String title;
    private String date;
    private String location;

    @Column(name = "wx_qrcode_path")
    private String wxQrcodePath;

    @Column(name = "ali_qrcode_path")
    private String aliQrcodePath;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getWxQrcodePath() { return wxQrcodePath; }
    public void setWxQrcodePath(String wxQrcodePath) { this.wxQrcodePath = wxQrcodePath; }
    public String getAliQrcodePath() { return aliQrcodePath; }
    public void setAliQrcodePath(String aliQrcodePath) { this.aliQrcodePath = aliQrcodePath; }
}

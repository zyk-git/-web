package com.example.giftbook.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gift_records")
public class GiftRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submit_id")
    private String submitId;
    private String name;
    private BigDecimal amount;
    private String relation;
    private String blessing;

    @Column(name = "payer_name")
    private String payerName;

    @Column(name = "payer_phone")
    private String payerPhone;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    private String ip;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubmitId() { return submitId; }
    public void setSubmitId(String submitId) { this.submitId = submitId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }
    public String getBlessing() { return blessing; }
    public void setBlessing(String blessing) { this.blessing = blessing; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public String getPayerPhone() { return payerPhone; }
    public void setPayerPhone(String payerPhone) { this.payerPhone = payerPhone; }
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}

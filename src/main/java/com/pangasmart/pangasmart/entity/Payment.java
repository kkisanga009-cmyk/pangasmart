package com.pangasmart.pangasmart.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long roomId;
    private String merchantReference;
    private String orderTrackingId;
    private Double amount;
    private String status; // PENDING, COMPLETED, FAILED
    private LocalDateTime createdAt;

    public Payment() {}

    public Payment(Long userId, Long roomId, String merchantReference, Double amount, String status) {
        this.userId = userId;
        this.roomId = roomId;
        this.merchantReference = merchantReference;
        this.amount = amount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getMerchantReference() { return merchantReference; }
    public void setMerchantReference(String merchantReference) { this.merchantReference = merchantReference; }
    public String getOrderTrackingId() { return orderTrackingId; }
    public void setOrderTrackingId(String orderTrackingId) { this.orderTrackingId = orderTrackingId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
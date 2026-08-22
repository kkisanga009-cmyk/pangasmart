package com.pangasmart.pangasmart.models;

import jakarta.persistence.*;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tenantEmail;  // Email ya Mpangaji anayebook
    private String tenantPhone;  // Namba ya Simu ya Mpangaji
    private Long roomId;         // ID ya chumba kilichobookiwa
    private String roomTitle;    // Aina ya chumba
    private String status;       // PENDING / APPROVED / REJECTED

    public Booking() {}

    public Booking(String tenantEmail, String tenantPhone, Long roomId, String roomTitle, String status) {
        this.tenantEmail = tenantEmail;
        this.tenantPhone = tenantPhone;
        this.roomId = roomId;
        this.roomTitle = roomTitle;
        this.status = status;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantEmail() { return tenantEmail; }
    public void setTenantEmail(String tenantEmail) { this.tenantEmail = tenantEmail; }

    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomTitle() { return roomTitle; }
    public void setRoomTitle(String roomTitle) { this.roomTitle = roomTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    // Ongeza hii field ndani ya class ya Booking
    private String landlordEmail;

    // Ongeza Getter na Setter hizi
    public String getLandlordEmail() {
        return landlordEmail;
    }

    public void setLandlordEmail(String landlordEmail) {
        this.landlordEmail = landlordEmail;
    }
}
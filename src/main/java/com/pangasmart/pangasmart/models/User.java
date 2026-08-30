package com.pangasmart.pangasmart.models;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String role;  // LANDLORD, TENANT, ADMIN
    private String status; // PENDING, APPROVED, REJECTED

    private boolean allowBooking;
    private double bookingFee;

    // Nyanja maalum kwa ajili ya kusimamia maombi ya online booking na ujumbe wa Admin
    private String bookingRequestStatus; // PENDING, APPROVED, REJECTED, REMOVED

    // Nyanja hii imeongezwa ili kuzuia lile kosa la Thymeleaf kwenye admin.html
    private String onlineBookingStatus;

    private String adminMessage;

    public User() {}

    // --- GETTERS & SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isAllowBooking() { return allowBooking; }
    public void setAllowBooking(boolean allowBooking) { this.allowBooking = allowBooking; }

    public double getBookingFee() { return bookingFee; }
    public void setBookingFee(double bookingFee) { this.bookingFee = bookingFee; }

    public String getBookingRequestStatus() { return bookingRequestStatus; }
    public void setBookingRequestStatus(String bookingRequestStatus) { this.bookingRequestStatus = bookingRequestStatus; }

    // Getter na Setter kwa ajili ya onlineBookingStatus
    public String getOnlineBookingStatus() { return onlineBookingStatus; }
    public void setOnlineBookingStatus(String onlineBookingStatus) { this.onlineBookingStatus = onlineBookingStatus; }

    public String getAdminMessage() { return adminMessage; }
    public void setAdminMessage(String adminMessage) { this.adminMessage = adminMessage; }
}
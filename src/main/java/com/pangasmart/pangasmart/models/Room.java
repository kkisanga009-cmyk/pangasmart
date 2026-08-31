package com.pangasmart.pangasmart.models;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String location;
    private Double price;
    private String status;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String videoUrl;

    private String landlordEmail;
    private String landlordPhone;
    private String landlordName;
    private boolean allowBooking;

    // [MB] Sehemu ya kuhifadhi kiasi cha fedha cha booking kilichowekwa na mwenyenyumba
    private Double bookingFee;

    // [MB] Alama ya kuonyesha kama chumba kimeshachukuliwa / kimefungwa
    private boolean booked;

    public Room() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getLandlordEmail() { return landlordEmail; }
    public void setLandlordEmail(String landlordEmail) { this.landlordEmail = landlordEmail; }

    public String getLandlordPhone() { return landlordPhone; }
    public void setLandlordPhone(String landlordPhone) { this.landlordPhone = landlordPhone; }

    public String getLandlordName() { return landlordName; }
    public void setLandlordName(String landlordName) { this.landlordName = landlordName; }

    public boolean isAllowBooking() { return allowBooking; }
    public void setAllowBooking(boolean allowBooking) { this.allowBooking = allowBooking; }

    // [MB] Getters na Setters kwa ajili ya bookingFee na booked
    public Double getBookingFee() { return bookingFee; }
    public void setBookingFee(Double bookingFee) { this.bookingFee = bookingFee; }

    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }
}
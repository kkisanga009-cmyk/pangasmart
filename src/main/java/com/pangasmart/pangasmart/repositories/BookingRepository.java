package com.pangasmart.pangasmart.repositories;

import com.pangasmart.pangasmart.models.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Method ya kuchuja bookings za Landlord maalum kwa kutumia email yake
    List<Booking> findByLandlordEmail(String landlordEmail);
}
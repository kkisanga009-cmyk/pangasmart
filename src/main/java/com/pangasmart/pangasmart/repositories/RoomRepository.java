package com.pangasmart.pangasmart.repositories;

import com.pangasmart.pangasmart.models.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Kupata vyumba vya Landlord kwa mfumo wa Pageable
    Page<Room> findByLandlordEmail(String landlordEmail, Pageable pageable);

    // Njia salama inayorudisha List ya vyumba bila kuvuruga Pageable iliyopo juu
    default List<Room> findAllByLandlordEmail(String landlordEmail) {
        return findByLandlordEmail(landlordEmail, Pageable.unpaged()).getContent();
    }

    // Kutafuta vyumba (Search) kwa Tenants kwa mfumo wa Pageable
    Page<Room> findByTitleContainingIgnoreCaseOrLocationContainingIgnoreCase(String title, String location, Pageable pageable);

    // Kutafuta na kuchuja vyumba vya Landlord mwenyewe akiwa anafanya search kwa mfumo wa Pageable
    @Query("SELECT r FROM Room r WHERE r.landlordEmail = :email AND (LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.location) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Room> searchLandlordRooms(@Param("email") String email, @Param("search") String search, Pageable pageable);
}
package com.pangasmart.pangasmart.repositories;

import com.pangasmart.pangasmart.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByLandlordEmail(String landlordEmail);

    List<Room> findByTitleContainingIgnoreCaseOrLocationContainingIgnoreCase(String title, String location);

    @Query("SELECT r FROM Room r WHERE r.landlordEmail = :email AND (LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.location) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Room> searchLandlordRooms(@Param("email") String email, @Param("search") String search);
}
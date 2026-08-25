package com.pangasmart.pangasmart.repository;

import com.pangasmart.pangasmart.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByMerchantReference(String merchantReference);

    boolean existsByUserIdAndRoomIdAndStatus(Long userId, Long roomId, String status);

    // Ongeza hii method hapa chini:
    List<Payment> findByUserIdAndStatus(Long userId, String status);
}
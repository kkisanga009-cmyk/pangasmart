package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.entity.Payment;
import com.pangasmart.pangasmart.repository.PaymentRepository;
import com.pangasmart.pangasmart.services.PesapalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PesapalService pesapalService;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/initiate")
    public String initiatePayment(@RequestParam("roomId") Long roomId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String userEmail = (String) session.getAttribute("userEmail");

        if (userId == null) {
            return "redirect:/login";
        }

        String merchantRef = UUID.randomUUID().toString();
        Payment payment = new Payment(userId, roomId, merchantRef, 1000.0, "PENDING");
        paymentRepository.save(payment);

        try {
            String token = pesapalService.getAuthToken();
            String redirectUrl = pesapalService.submitOrder(token, merchantRef, 1000.0, userEmail, "0700000000");
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            return "redirect:/home?error=PaymentFailed";
        }
    }

    @GetMapping("/callback")
    public String paymentCallback(@RequestParam("OrderMerchantReference") String merchantRef,
                                  @RequestParam("OrderTrackingId") String trackingId) {
        Payment payment = paymentRepository.findByMerchantReference(merchantRef).orElse(null);
        if (payment != null) {
            payment.setOrderTrackingId(trackingId);
            payment.setStatus("COMPLETED");
            paymentRepository.save(payment);
        }
        return "redirect:/home?success=PaymentSuccessful";
    }
}
package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.models.Payment;
import com.pangasmart.pangasmart.repository.PaymentRepository;
import com.pangasmart.pangasmart.services.PesapalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PesapalService pesapalService;

    // 1. Kuanzisha Process ya Malipo na Kuelekeza Pesapal
    @PostMapping("/initiate")
    public String initiatePayment(@RequestParam Double amount,
                                  @RequestParam(required = false) String email,
                                  @RequestParam(required = false) String phone,
                                  Model model) {
        try {
            // Hatua ya A: Pata Token kutoka Pesapal
            String token = pesapalService.getAuthToken();
            if (token == null) {
                model.addAttribute("error", "Imefeli kupata mawasiliano na mfumo wa Pesapal.");
                return "error";
            }

            // Hatua ya B: Tengeneza Reference ya kipekee ya Muamala
            String merchantRef = "PS-" + UUID.randomUUID().toString().substring(0, 8);

            // Hatua ya C: Tuma ombi la oda Pesapal na upate Redirect Link
            String redirectUrl = pesapalService.submitOrder(token, merchantRef, amount, email, phone);

            if (redirectUrl != null) {
                // Hifadhi Taarifa za Awali za Malipo kwenye Database (Status: PENDING)
                Payment payment = new Payment();
                payment.setAmount(amount);
                payment.setMerchantReference(merchantRef);
                payment.setStatus("PENDING");
                paymentRepository.save(payment);

                // Elekeza mteja kwenye ukurasa wa malipo wa Pesapal
                return "redirect:" + redirectUrl;
            } else {
                model.addAttribute("error", "Imefeli kutengeneza ombi la malipo Pesapal.");
                return "error";
            }

        } catch (Exception e) {
            model.addAttribute("error", "Kuna hitilafu imetokea: " + e.getMessage());
            return "error";
        }
    }

    // 2. Callback Endpoint - Pesapal inamrejesha mtumiaji hapa baada ya kulipia
    @GetMapping("/callback")
    public String handleCallback(@RequestParam(value = "OrderTrackingId", required = false) String orderTrackingId,
                                 @RequestParam(value = "OrderMerchantReference", required = false) String merchantRef) {

        if (merchantRef != null) {
            Optional<Payment> paymentOpt = paymentRepository.findByMerchantReference(merchantRef);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setOrderTrackingId(orderTrackingId);
                payment.setStatus("COMPLETED");
                paymentRepository.save(payment);
            }
        }

        return "redirect:/dashboard?payment=success";
    }

    // 3. Save Payment ya kawaida
    @PostMapping("/save")
    public String savePayment(@ModelAttribute Payment payment) {
        paymentRepository.save(payment);
        return "redirect:/dashboard";
    }
}
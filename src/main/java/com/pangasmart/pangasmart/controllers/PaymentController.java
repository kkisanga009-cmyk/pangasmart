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
            String token = pesapalService.getAuthToken();
            if (token == null) {
                model.addAttribute("error", "Imefeli kupata mawasiliano na mfumo wa Pesapal.");
                return "error";
            }

            String merchantRef = "PS-" + UUID.randomUUID().toString().substring(0, 8);
            String redirectUrl = pesapalService.submitOrder(token, merchantRef, amount, email, phone);

            if (redirectUrl != null) {
                Payment payment = new Payment();
                payment.setAmount(amount);
                payment.setMerchantReference(merchantRef);
                payment.setStatus("PENDING");
                paymentRepository.save(payment);

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

        if (merchantRef != null && orderTrackingId != null) {
            // Pata Auth Token kwanza
            String token = pesapalService.getAuthToken();

            // Uliza Pesapal Status ya Muamala huu halisi
            String actualStatus = pesapalService.getTransactionStatus(token, orderTrackingId);

            Optional<Payment> paymentOpt = paymentRepository.findByMerchantReference(merchantRef);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setOrderTrackingId(orderTrackingId);

                // Badilisha status tu kama Pesapal imethibitisha kuwa "Completed"
                if ("Completed".equalsIgnoreCase(actualStatus)) {
                    payment.setStatus("COMPLETED");
                    paymentRepository.save(payment);
                    return "redirect:/dashboard?payment=success";
                } else {
                    payment.setStatus("FAILED");
                    paymentRepository.save(payment);
                    return "redirect:/dashboard?payment=failed";
                }
            }
        }

        return "redirect:/dashboard?payment=pending";
    }

    // 3. Save Payment ya kawaida
    @PostMapping("/save")
    public String savePayment(@ModelAttribute Payment payment) {
        paymentRepository.save(payment);
        return "redirect:/dashboard";
    }
}
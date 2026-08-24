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

    @GetMapping("/callback")
    public String handleCallback(@RequestParam(value = "OrderTrackingId", required = false) String orderTrackingId,
                                 @RequestParam(value = "OrderMerchantReference", required = false) String merchantRef,
                                 @RequestParam(value = "orderTrackingId", required = false) String altTrackingId,
                                 @RequestParam(value = "orderMerchantReference", required = false) String altMerchantRef) {

        // Kuchukua parameters ziwe kwa herufi kubwa au ndogo
        String trackingId = (orderTrackingId != null) ? orderTrackingId : altTrackingId;
        String ref = (merchantRef != null) ? merchantRef : altMerchantRef;

        System.out.println("Callback Triggered - TrackingId: " + trackingId + " | Ref: " + ref);

        if (ref != null && trackingId != null) {
            String token = pesapalService.getAuthToken();
            String actualStatus = pesapalService.getTransactionStatus(token, trackingId);

            Optional<Payment> paymentOpt = paymentRepository.findByMerchantReference(ref);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setOrderTrackingId(trackingId);

                // HAPA NDIPO ULINZI ULIPO:
                // Tutaweka status kuwa COMPLETED iwapo tu Pesapal imethibitisha ni "COMPLETED"
                if ("COMPLETED".equalsIgnoreCase(actualStatus)) {
                    payment.setStatus("COMPLETED");
                    paymentRepository.save(payment);
                    return "redirect:/dashboard?payment=success";
                } else if ("FAILED".equalsIgnoreCase(actualStatus) || "INVALID".equalsIgnoreCase(actualStatus)) {
                    payment.setStatus("FAILED");
                    paymentRepository.save(payment);
                    return "redirect:/dashboard?payment=failed";
                } else {
                    // Kama status bado ni PENDING au haijatambulika, HATUSAVE COMPLETED!
                    payment.setStatus("PENDING");
                    paymentRepository.save(payment);
                    return "redirect:/dashboard?payment=pending";
                }
            }
        }

        return "redirect:/dashboard?payment=invalid";
    }

    @PostMapping("/save")
    public String savePayment(@ModelAttribute Payment payment) {
        paymentRepository.save(payment);
        return "redirect:/dashboard";
    }
}
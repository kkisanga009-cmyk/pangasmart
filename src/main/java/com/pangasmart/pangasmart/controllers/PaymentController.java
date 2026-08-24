package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.models.Payment;
import com.pangasmart.pangasmart.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/save")
    public String savePayment(@ModelAttribute Payment payment) {
        paymentRepository.save(payment);
        return "redirect:/dashboard";
    }
}
package com.pangasmart.pangasmart.controllers;

// 1. REKEBISHA IMPORT HAPA: Tumia models.Payment badala ya entity.Payment
import com.pangasmart.pangasmart.models.Payment;
import com.pangasmart.pangasmart.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    // Hakikisha njia zako zote ndani ya controller hii zina tumia models.Payment
}
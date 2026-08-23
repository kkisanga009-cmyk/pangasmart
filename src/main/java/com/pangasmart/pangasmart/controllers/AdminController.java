package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.repository.PaymentRepository;
import com.pangasmart.pangasmart.repositories.UserRepository; // Hakikisha unayo
import com.pangasmart.pangasmart.repositories.RoomRepository; // Hakikisha unayo
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        String role = (String) session.getAttribute("userRole");

        // Ulinzi: Mtu asiye Admin asifungue
        if (role == null || !role.equals("ADMIN")) {
            return "redirect:/home";
        }

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalRooms", roomRepository.count());
        model.addAttribute("payments", paymentRepository.findAll());
        model.addAttribute("totalRevenue", paymentRepository.findAll().stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .mapToDouble(p -> p.getAmount()).sum());

        return "admin";
    }
}
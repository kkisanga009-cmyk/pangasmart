package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.models.User;
import com.pangasmart.pangasmart.repository.PaymentRepository;
import com.pangasmart.pangasmart.repositories.UserRepository;
import com.pangasmart.pangasmart.repositories.RoomRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

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
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");

        // Ulinzi: Kama hajainingia au si Admin (pia inaangalia Email kama ulinzi wa ziada)
        boolean isAdmin = (role != null && role.equalsIgnoreCase("ADMIN")) ||
                (loggedInUser != null && loggedInUser.getRole() != null && loggedInUser.getRole().equalsIgnoreCase("ADMIN")) ||
                (loggedInUser != null && loggedInUser.getEmail() != null && loggedInUser.getEmail().equals("kkisanga009@gmail.com"));

        if (!isAdmin) {
            return "redirect:/home";
        }

        // Tenganisha Watumiaji kwa Role
        List<User> tenants = userRepository.findByRole("TENANT");
        List<User> landlords = userRepository.findByRole("LANDLORD");

        // Hesabu Mapato ya SUCCESS au COMPLETED
        Double totalRevenue = paymentRepository.findAll().stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()) || "COMPLETED".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalRooms", roomRepository.count());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("payments", paymentRepository.findAll());

        // Ongeza Tenants na Landlords kwenye Model
        model.addAttribute("tenants", tenants);
        model.addAttribute("landlords", landlords);

        return "admin";
    }
}
package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.models.User;
import com.pangasmart.pangasmart.repository.PaymentRepository;
import com.pangasmart.pangasmart.repositories.UserRepository;
import com.pangasmart.pangasmart.repositories.RoomRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // Helper Method ya Kukagua kama Mtu ni Admin
    private boolean checkIsAdmin(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");

        return (role != null && role.equalsIgnoreCase("ADMIN")) ||
                (loggedInUser != null && loggedInUser.getRole() != null && loggedInUser.getRole().equalsIgnoreCase("ADMIN")) ||
                (loggedInUser != null && loggedInUser.getEmail() != null && loggedInUser.getEmail().equals("kkisanga009@gmail.com"));
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        if (!checkIsAdmin(session)) {
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

        model.addAttribute("tenants", tenants);
        model.addAttribute("landlords", landlords);

        return "admin";
    }

    // 1. KUFUTA MTUMIAJI (DELETE USER)
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsAdmin(session)) {
            return "redirect:/home";
        }
        userRepository.deleteById(id);
        return "redirect:/admin/dashboard?deleted=true";
    }

    // 2. KUFUNGUA FORM YA EDIT USER
    @GetMapping("/admin/users/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, HttpSession session, Model model) {
        if (!checkIsAdmin(session)) {
            return "redirect:/home";
        }

        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            model.addAttribute("user", userOptional.get());
            return "edit-user"; // Tutaunda ukurasa mdogo wa edit-user.html au Modal
        }

        return "redirect:/admin/dashboard";
    }

    // 3. KUHIFADHI MABADILIKO YA MTUMIAJI (SAVE EDITED USER)
    @PostMapping("/admin/users/update")
    public String updateUser(@ModelAttribute("user") User updatedUser, HttpSession session) {
        if (!checkIsAdmin(session)) {
            return "redirect:/home";
        }

        Optional<User> existingUserOpt = userRepository.findById(updatedUser.getId());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            existingUser.setFullName(updatedUser.getFullName());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setPhone(updatedUser.getPhone());
            existingUser.setPassword(updatedUser.getPassword());
            existingUser.setRole(updatedUser.getRole());

            userRepository.save(existingUser);
        }

        return "redirect:/admin/dashboard?updated=true";
    }
}
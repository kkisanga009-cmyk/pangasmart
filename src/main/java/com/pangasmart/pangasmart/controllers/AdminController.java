package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.models.Payment;
import com.pangasmart.pangasmart.models.Room;
import com.pangasmart.pangasmart.models.User;
import com.pangasmart.pangasmart.repository.PaymentRepository;
import com.pangasmart.pangasmart.repositories.RoomRepository;
import com.pangasmart.pangasmart.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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

    private boolean checkIsAdmin(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");

        return (role != null && role.equalsIgnoreCase("ADMIN")) ||
                (loggedInUser != null && loggedInUser.getRole() != null && loggedInUser.getRole().equalsIgnoreCase("ADMIN")) ||
                (loggedInUser != null && loggedInUser.getEmail() != null && loggedInUser.getEmail().equals("kkisanga009@gmail.com"));
    }

    public static class PaymentDetailDTO {
        private Long paymentId;
        private String tenantName;
        private String tenantPhone;
        private String roomTitle;
        private String landlordName;
        private String landlordPhone;
        private Double amount;
        private String status;
        private LocalDateTime date;
        private String formattedDate;

        public PaymentDetailDTO(Long paymentId, String tenantName, String tenantPhone, String roomTitle, String landlordName, String landlordPhone, Double amount, String status, LocalDateTime date) {
            this.paymentId = paymentId;
            this.tenantName = tenantName;
            this.tenantPhone = tenantPhone;
            this.roomTitle = roomTitle;
            this.landlordName = landlordName;
            this.landlordPhone = landlordPhone;
            this.amount = amount;
            this.status = status;
            this.date = date;

            // Format tarehe kuwa vizuri (Mfano: 24/08/2026 14:30)
            if (date != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                this.formattedDate = date.format(formatter);
            } else {
                this.formattedDate = "N/A";
            }
        }

        public Long getPaymentId() { return paymentId; }
        public String getTenantName() { return tenantName; }
        public String getTenantPhone() { return tenantPhone; }
        public String getRoomTitle() { return roomTitle; }
        public String getLandlordName() { return landlordName; }
        public String getLandlordPhone() { return landlordPhone; }
        public Double getAmount() { return amount; }
        public String getStatus() { return status; }
        public LocalDateTime getDate() { return date; }
        public String getFormattedDate() { return formattedDate; }
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        if (!checkIsAdmin(session)) {
            return "redirect:/home";
        }

        List<User> tenants = userRepository.findByRole("TENANT");
        List<User> landlords = userRepository.findByRole("LANDLORD");
        List<Room> rooms = roomRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();

        // Kupanga malipo kuanzia ya hivi karibuni (Latest First)
        allPayments.sort((p1, p2) -> {
            if (p1.getPaymentDate() == null) return 1;
            if (p2.getPaymentDate() == null) return -1;
            return p2.getPaymentDate().compareTo(p1.getPaymentDate());
        });

        Double totalRevenue = allPayments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()) || "COMPLETED".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();

        List<PaymentDetailDTO> paymentDetails = new ArrayList<>();
        for (Payment p : allPayments) {
            User tenant = (p.getUserId() != null) ? userRepository.findById(p.getUserId()).orElse(null) : null;
            Room room = (p.getRoomId() != null) ? roomRepository.findById(p.getRoomId()).orElse(null) : null;

            User landlord = null;
            if (landlords != null && !landlords.isEmpty()) {
                landlord = landlords.get(0);
            }

            String tenantName = tenant != null ? tenant.getFullName() : "N/A";
            String tenantPhone = tenant != null ? tenant.getPhone() : "N/A";
            String roomTitle = room != null ? room.getTitle() : "Chumba ID: " + p.getRoomId();
            String landlordName = landlord != null ? landlord.getFullName() : "N/A";
            String landlordPhone = landlord != null ? landlord.getPhone() : "N/A";

            paymentDetails.add(new PaymentDetailDTO(
                    p.getId(), tenantName, tenantPhone, roomTitle, landlordName, landlordPhone,
                    p.getAmount(), p.getStatus(), p.getPaymentDate()
            ));
        }

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalRooms", roomRepository.count());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("paymentDetails", paymentDetails);
        model.addAttribute("tenants", tenants);
        model.addAttribute("landlords", landlords);
        model.addAttribute("rooms", rooms);

        return "admin";
    }

    @GetMapping("/admin/rooms/delete/{id}")
    public String deleteRoom(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsAdmin(session)) {
            return "redirect:/home";
        }
        roomRepository.deleteById(id);
        return "redirect:/admin/dashboard?roomDeleted=true";
    }

    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsAdmin(session)) {
            return "redirect:/home";
        }
        userRepository.deleteById(id);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/users/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, HttpSession session, Model model) {
        if (!checkIsAdmin(session)) {
            return "redirect:/home";
        }

        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            model.addAttribute("user", userOptional.get());
            return "edit-user";
        }

        return "redirect:/admin/dashboard";
    }

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

        return "redirect:/admin/dashboard";
    }
}
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private boolean checkIsAnyAdmin(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            loggedInUser = (User) session.getAttribute("currentUser");
        }
        String role = (String) session.getAttribute("userRole");
        return (role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("SUB_ADMIN"))) ||
                (loggedInUser != null && loggedInUser.getRole() != null &&
                        (loggedInUser.getRole().equalsIgnoreCase("ADMIN") || loggedInUser.getRole().equalsIgnoreCase("SUB_ADMIN"))) ||
                (loggedInUser != null && loggedInUser.getEmail() != null && loggedInUser.getEmail().equalsIgnoreCase("kkisanga009@gmail.com"));
    }

    private boolean checkIsSuperAdmin(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            loggedInUser = (User) session.getAttribute("currentUser");
        }
        String role = (String) session.getAttribute("userRole");
        return (role != null && role.equalsIgnoreCase("ADMIN")) ||
                (loggedInUser != null && loggedInUser.getRole() != null && loggedInUser.getRole().equalsIgnoreCase("ADMIN")) ||
                (loggedInUser != null && loggedInUser.getEmail() != null && loggedInUser.getEmail().equalsIgnoreCase("kkisanga009@gmail.com"));
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

    public static class MonthlyRevenueDTO {
        private String monthName;
        private int year;
        private Double amount;

        public MonthlyRevenueDTO(String monthName, int year, Double amount) {
            this.monthName = monthName;
            this.year = year;
            this.amount = amount;
        }

        public String getMonthName() { return monthName; }
        public int getYear() { return year; }
        public Double getAmount() { return amount; }
    }

    @GetMapping({"/admin", "/admin/dashboard"})
    public String adminDashboard(HttpSession session, Model model) {
        if (!checkIsAnyAdmin(session)) {
            return "redirect:/login";
        }

        User loggedUser = (User) session.getAttribute("loggedInUser");
        if (loggedUser == null) {
            loggedUser = (User) session.getAttribute("currentUser");
        }

        boolean isSuperAdmin = checkIsSuperAdmin(session);

        model.addAttribute("currentUser", loggedUser);
        model.addAttribute("loggedInUser", loggedUser);
        model.addAttribute("isSuperAdmin", isSuperAdmin);

        List<User> tenants = userRepository.findByRole("TENANT");
        List<User> landlords = userRepository.findByRole("LANDLORD");
        List<User> subAdmins = userRepository.findByRole("SUB_ADMIN");
        List<Room> rooms = roomRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();

        if (allPayments != null && !allPayments.isEmpty()) {
            allPayments.sort((p1, p2) -> {
                if (p1.getPaymentDate() == null) return 1;
                if (p2.getPaymentDate() == null) return -1;
                return p2.getPaymentDate().compareTo(p1.getPaymentDate());
            });
        }

        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        Double totalRevenue = 0.0;
        double monthlyRevenue = 0.0;
        double yearlyRevenue = 0.0;

        Map<String, Double> monthlyRevenueMap = new LinkedHashMap<>();
        String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

        for (String mName : monthNames) {
            monthlyRevenueMap.put(mName + "-" + currentYear, 0.0);
        }

        List<PaymentDetailDTO> paymentDetails = new ArrayList<>();
        if (allPayments != null) {
            for (Payment p : allPayments) {
                if (p == null) continue;

                boolean isSuccess = "SUCCESS".equalsIgnoreCase(p.getStatus()) || "COMPLETED".equalsIgnoreCase(p.getStatus());

                if (isSuccess && p.getAmount() != null) {
                    totalRevenue += p.getAmount();
                }

                if (p.getPaymentDate() != null) {
                    int pYear = p.getPaymentDate().getYear();
                    int pMonth = p.getPaymentDate().getMonthValue();

                    if (pYear == currentYear && isSuccess && p.getAmount() != null) {
                        yearlyRevenue += p.getAmount();
                        String mKey = monthNames[pMonth - 1] + "-" + pYear;
                        monthlyRevenueMap.put(mKey, monthlyRevenueMap.getOrDefault(mKey, 0.0) + p.getAmount());
                    }

                    if (pMonth == currentMonth && pYear == currentYear) {
                        if (isSuccess && p.getAmount() != null) {
                            monthlyRevenue += p.getAmount();
                        }
                    }
                }

                User tenant = (p.getUserId() != null) ? userRepository.findById(p.getUserId()).orElse(null) : null;
                Room room = (p.getRoomId() != null) ? roomRepository.findById(p.getRoomId()).orElse(null) : null;

                User landlord = null;
                if (room != null && room.getLandlordEmail() != null) {
                    try {
                        List<User> foundLandlords = userRepository.findAllByEmail(room.getLandlordEmail());
                        if (foundLandlords != null && !foundLandlords.isEmpty()) {
                            landlord = foundLandlords.get(0);
                        }
                    } catch (Exception e) {
                        landlord = userRepository.findByEmail(room.getLandlordEmail()).orElse(null);
                    }
                }

                String tenantName = tenant != null ? tenant.getFullName() : "N/A";
                String tenantPhone = tenant != null ? tenant.getPhone() : "N/A";
                String roomTitle = room != null ? room.getTitle() : "Chumba ID: " + p.getRoomId();
                String landlordName = landlord != null ? landlord.getFullName() : "N/A";
                String landlordPhone = (room != null && room.getLandlordPhone() != null && !room.getLandlordPhone().isEmpty())
                        ? room.getLandlordPhone()
                        : (landlord != null ? landlord.getPhone() : "N/A");

                paymentDetails.add(new PaymentDetailDTO(
                        p.getId(), tenantName, tenantPhone, roomTitle, landlordName, landlordPhone,
                        p.getAmount(), p.getStatus(), p.getPaymentDate()
                ));
            }
        }

        List<MonthlyRevenueDTO> monthlyRevenueList = new ArrayList<>();
        for (int i = 0; i < monthNames.length; i++) {
            String key = monthNames[i] + "-" + currentYear;
            Double rev = monthlyRevenueMap.getOrDefault(key, 0.0);
            monthlyRevenueList.add(new MonthlyRevenueDTO(monthNames[i], currentYear, rev));
        }

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalRooms", roomRepository.count());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("yearlyRevenue", yearlyRevenue);
        model.addAttribute("monthlyRevenueList", monthlyRevenueList);
        model.addAttribute("paymentDetails", paymentDetails);
        model.addAttribute("tenants", tenants != null ? tenants : new ArrayList<>());
        model.addAttribute("landlords", landlords != null ? landlords : new ArrayList<>());
        model.addAttribute("subAdmins", subAdmins != null ? subAdmins : new ArrayList<>());
        model.addAttribute("rooms", rooms != null ? rooms : new ArrayList<>());

        return "admin";
    }

    @GetMapping("/admin/users/approve/{id}")
    public String approveLandlord(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsSuperAdmin(session)) { return "redirect:/admin/dashboard"; }
        userRepository.findById(id).ifPresent(user -> { user.setStatus("APPROVED"); userRepository.save(user); });
        return "redirect:/admin/dashboard?approved=true";
    }

    @GetMapping("/admin/users/reject/{id}")
    public String rejectLandlord(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsSuperAdmin(session)) { return "redirect:/admin/dashboard"; }
        userRepository.findById(id).ifPresent(user -> { user.setStatus("REJECTED"); userRepository.save(user); });
        return "redirect:/admin/dashboard?rejected=true";
    }

    @GetMapping("/admin/rooms/delete/{id}")
    public String deleteRoom(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsSuperAdmin(session)) { return "redirect:/admin/dashboard"; }
        roomRepository.deleteById(id);
        return "redirect:/admin/dashboard?roomDeleted=true";
    }

    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsSuperAdmin(session)) { return "redirect:/admin/dashboard"; }
        userRepository.deleteById(id);
        return "redirect:/admin/dashboard?userDeleted=true";
    }

    @GetMapping("/admin/users/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, HttpSession session, Model model) {
        if (!checkIsSuperAdmin(session)) { return "redirect:/admin/dashboard"; }
        userRepository.findById(id).ifPresent(user -> model.addAttribute("user", user));
        return "edit-user";
    }

    @PostMapping("/admin/users/update")
    public String updateUser(@ModelAttribute("user") User updatedUser, HttpSession session) {
        if (!checkIsSuperAdmin(session)) { return "redirect:/admin/dashboard"; }
        userRepository.findById(updatedUser.getId()).ifPresent(existingUser -> {
            existingUser.setFullName(updatedUser.getFullName());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setPhone(updatedUser.getPhone());
            existingUser.setPassword(updatedUser.getPassword());
            existingUser.setRole(updatedUser.getRole());
            if (updatedUser.getStatus() != null && !updatedUser.getStatus().trim().isEmpty()) {
                existingUser.setStatus(updatedUser.getStatus());
            }
            userRepository.save(existingUser);
        });
        return "redirect:/admin/dashboard?userUpdated=true";
    }
}
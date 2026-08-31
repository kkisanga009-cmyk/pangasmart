package com.pangasmart.pangasmart.controllers;
import com.pangasmart.pangasmart.models.Booking;
import com.pangasmart.pangasmart.models.Payment;
import com.pangasmart.pangasmart.models.Room;
import com.pangasmart.pangasmart.models.User;
import com.pangasmart.pangasmart.repository.PaymentRepository;
import com.pangasmart.pangasmart.repositories.BookingRepository;
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

@Controller
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

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

        List<User> landlordBookingRequests = new ArrayList<>();
        if (landlords != null) {
            for (User l : landlords) {
                if (l.isAllowBooking() || "PENDING".equalsIgnoreCase(l.getBookingRequestStatus())) {
                    landlordBookingRequests.add(l);
                }
            }
        }

        List<User> allUsers = userRepository.findAll();
        List<User> allUsersList = new ArrayList<>();
        if (allUsers != null) {
            for (User u : allUsers) {
                if (u.getRole() == null || (!u.getRole().equalsIgnoreCase("ADMIN") && !u.getRole().equalsIgnoreCase("SUB_ADMIN"))) {
                    allUsersList.add(u);
                }
            }
        }

        List<Room> rooms = roomRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();

        if (allPayments != null && !allPayments.isEmpty()) {
            allPayments.sort((p1, p2) -> {
                if (p1.getPaymentDate() == null) return 1;
                if (p2.getPaymentDate() == null) return -1;
                return p2.getPaymentDate().compareTo(p1.getPaymentDate());
            });
        }

        List<Booking> bookingList = bookingRepository.findAll();
        if (bookingList != null && !bookingList.isEmpty()) {
            bookingList.sort((b1, b2) -> {
                if (b1.getCreatedAt() == null) return 1;
                if (b2.getCreatedAt() == null) return -1;
                return b2.getCreatedAt().compareTo(b1.getCreatedAt());
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
        model.addAttribute("landlordBookingRequests", landlordBookingRequests);
        model.addAttribute("subAdmins", subAdmins != null ? subAdmins : new ArrayList<>());
        model.addAttribute("allUsersList", allUsersList);
        model.addAttribute("rooms", rooms != null ? rooms : new ArrayList<>());
        model.addAttribute("bookingList", bookingList != null ? bookingList : new ArrayList<>());

        return "admin";
    }

    // [MB] Endpoints za Usimamizi wa Maombi ya Booking ya Wapangaji (Tenant Bookings)
    @GetMapping("/admin/booking/accept/{id}")
    public String acceptBooking(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsAnyAdmin(session)) {
            return "redirect:/login";
        }
        bookingRepository.findById(id).ifPresent(booking -> {
            booking.setStatus("APPROVED");
            bookingRepository.save(booking);

            // [MB] Kitendo cha ku-approve kinapofanyika, chumba kinapigwa kufuli (Locked) au alama yake inabadilishwa kuwa booked
            if (booking.getRoomId() != null) {
                roomRepository.findById(booking.getRoomId()).ifPresent(room -> {
                    room.setBooked(true);
                    roomRepository.save(room);
                });
            }
        });
        return "redirect:/admin/dashboard?bookingAccepted=true";
    }

    @GetMapping("/admin/booking/reject/{id}")
    public String rejectBooking(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsAnyAdmin(session)) {
            return "redirect:/login";
        }
        bookingRepository.findById(id).ifPresent(booking -> {
            booking.setStatus("REJECTED");
            bookingRepository.save(booking);
        });
        return "redirect:/admin/dashboard?bookingRejected=true";
    }

    @GetMapping("/admin/booking/complete/{id}")
    public String completeBooking(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsAnyAdmin(session)) {
            return "redirect:/login";
        }
        bookingRepository.findById(id).ifPresent(booking -> {
            booking.setStatus("APPROVED");
            bookingRepository.save(booking);

            if (booking.getRoomId() != null) {
                try {
                    roomRepository.deleteById(booking.getRoomId());
                } catch (Exception e) {
                    // Ignore kama chumba hakipo
                }
            }
        });
        return "redirect:/admin/dashboard?bookingCompleted=true";
    }

    // --- SEHEMU YA KUSIMAMIA MAOMBI YA ONLINE BOOKING YA WENYENYUMBA ---

    @GetMapping("/admin/landlord-booking/approve/{id}")
    public String approveLandlordBooking(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsAnyAdmin(session)) { return "redirect:/login"; }

        userRepository.findById(id).ifPresent(landlord -> {
            landlord.setAllowBooking(true);
            landlord.setBookingRequestStatus("APPROVED");
            landlord.setAdminMessage("Ombi lako limekubaliwa! Sasa wapangaji watafanya booking online kupitia Lipa Namba 350213373, na fedha zako zitatumwa na Admin kwako moja kwa moja.");
            userRepository.save(landlord);
        });

        return "redirect:/admin/dashboard?landlordBookingApproved=true";
    }

    @GetMapping("/admin/landlord-booking/reject/{id}")
    public String rejectLandlordBooking(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsAnyAdmin(session)) { return "redirect:/login"; }
        userRepository.findById(id).ifPresent(landlord -> {
            landlord.setAllowBooking(false);
            landlord.setBookingRequestStatus("REJECTED");
            landlord.setAdminMessage("Samahani, hujakidhi vigezo, hivyo ombi lako limekataliwa.");
            userRepository.save(landlord);
        });
        return "redirect:/admin/dashboard?landlordBookingRejected=true";
    }

    @GetMapping("/admin/landlord-booking/remove/{id}")
    public String removeLandlordBooking(@PathVariable("id") Long id, HttpSession session) {
        if (!checkIsAnyAdmin(session)) { return "redirect:/login"; }
        userRepository.findById(id).ifPresent(landlord -> {
            landlord.setAllowBooking(false);
            landlord.setBookingRequestStatus("REMOVED");
            landlord.setAdminMessage("Admin ameondoa booking za online. Wasiliana naye kwa taarifa zaidi.");
            userRepository.save(landlord);
        });
        return "redirect:/admin/dashboard?landlordBookingRemoved=true";
    }

    // Njia ya kushughulikia kuongeza Sub-Admin kupitia Modal
    @PostMapping("/admin/sub-admins/add")
    public String addSubAdmin(@RequestParam("userId") Long userId, HttpSession session) {
        if (!checkIsSuperAdmin(session)) {
            return "redirect:/admin/dashboard";
        }

        userRepository.findById(userId).ifPresent(user -> {
            user.setRole("SUB_ADMIN");
            userRepository.save(user);
        });

        return "redirect:/admin/dashboard?subAdminAdded=true";
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
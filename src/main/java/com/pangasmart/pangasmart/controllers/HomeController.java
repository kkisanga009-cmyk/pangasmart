package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.models.Booking;
import com.pangasmart.pangasmart.models.Payment;
import com.pangasmart.pangasmart.models.Room;
import com.pangasmart.pangasmart.models.User;
import com.pangasmart.pangasmart.repositories.BookingRepository;
import com.pangasmart.pangasmart.repository.PaymentRepository;
import com.pangasmart.pangasmart.repositories.RoomRepository;
import com.pangasmart.pangasmart.repositories.UserRepository;
import com.pangasmart.pangasmart.services.PesapalService;
import com.pangasmart.pangasmart.services.SmsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class HomeController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SmsService smsService;

    @Autowired
    private PesapalService pesapalService; // Tumeongeza PesapalService hapa

    @GetMapping("/home")
    public String showHomePage(Model model,
                               HttpSession session,
                               @RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "paidRoomId", required = false) Long paidRoomId) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        String userRole = (String) session.getAttribute("userRole");
        if (userRole == null && user.getRole() != null) {
            userRole = user.getRole();
            session.setAttribute("userRole", userRole);
        }

        List<Room> roomList;
        List<Booking> bookingList;

        if ("LANDLORD".equalsIgnoreCase(userRole)) {
            if (search != null && !search.trim().isEmpty()) {
                roomList = roomRepository.searchLandlordRooms(user.getEmail(), search);
            } else {
                roomList = roomRepository.findByLandlordEmail(user.getEmail());
            }
            bookingList = bookingRepository.findByLandlordEmail(user.getEmail());
        } else {
            if (search != null && !search.trim().isEmpty()) {
                roomList = roomRepository.findByTitleContainingIgnoreCaseOrLocationContainingIgnoreCase(search, search);
            } else {
                roomList = roomRepository.findAll();
            }
            bookingList = new ArrayList<>();
        }

        // Kagua kama mtumiaji alishalipia chumba hiki huko nyuma (COMPLETED status)
        if (paidRoomId != null) {
            boolean isPaid = paymentRepository.existsByUserIdAndRoomIdAndStatus(user.getId(), paidRoomId, "COMPLETED");
            if (isPaid) {
                Optional<Room> roomOpt = roomRepository.findById(paidRoomId);
                if (roomOpt.isPresent()) {
                    Room paidRoom = roomOpt.get();
                    String phone = paidRoom.getLandlordPhone();

                    if (phone == null || phone.trim().isEmpty()) {
                        Optional<User> landlordOpt = userRepository.findByEmail(paidRoom.getLandlordEmail());
                        if (landlordOpt.isPresent() && landlordOpt.get().getPhone() != null) {
                            phone = landlordOpt.get().getPhone();
                        } else {
                            phone = paidRoom.getLandlordEmail();
                        }
                    }
                    model.addAttribute("paidRoomId", paidRoomId);
                    model.addAttribute("landlordPhone", phone);
                }
            }
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("userRole", userRole != null ? userRole : "TENANT");
        model.addAttribute("rooms", roomList != null ? roomList : new ArrayList<>());
        model.addAttribute("bookings", bookingList != null ? bookingList : new ArrayList<>());
        model.addAttribute("searchKeyword", search);

        return "home";
    }

    @PostMapping("/add-room")
    public String addRoom(@ModelAttribute Room room,
                          @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                          @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
                          HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        room.setLandlordEmail(user.getEmail());

        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            room.setLandlordPhone(user.getPhone());
        }

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
                String imageUrl = "data:" + imageFile.getContentType() + ";base64," + base64Image;
                room.setImageUrl(imageUrl);
            }

            if (videoFile != null && !videoFile.isEmpty()) {
                String base64Video = Base64.getEncoder().encodeToString(videoFile.getBytes());
                String videoUrl = "data:" + videoFile.getContentType() + ";base64," + base64Video;
                room.setVideoUrl(videoUrl);
            }
        } catch (Exception e) {
            System.err.println("Base64 Encoding Error: " + e.getMessage());
            e.printStackTrace();
        }

        roomRepository.save(room);
        return "redirect:/home?roomAdded=true";
    }

    @GetMapping("/landlord/rooms/delete-image/{id}")
    public String deleteRoomImage(@PathVariable("id") Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Optional<Room> roomOptional = roomRepository.findById(id);
        if (roomOptional.isPresent()) {
            Room room = roomOptional.get();
            if (room.getLandlordEmail() != null && room.getLandlordEmail().equalsIgnoreCase(loggedInUser.getEmail())) {
                room.setImageUrl(null);
                roomRepository.save(room);
            }
        }
        return "redirect:/home?imageDeleted=true";
    }

    @GetMapping("/landlord/rooms/delete-video/{id}")
    public String deleteRoomVideo(@PathVariable("id") Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Optional<Room> roomOptional = roomRepository.findById(id);
        if (roomOptional.isPresent()) {
            Room room = roomOptional.get();
            if (room.getLandlordEmail() != null && room.getLandlordEmail().equalsIgnoreCase(loggedInUser.getEmail())) {
                room.setVideoUrl(null);
                roomRepository.save(room);
            }
        }
        return "redirect:/home?videoDeleted=true";
    }

    @GetMapping("/landlord/rooms/delete/{id}")
    public String deleteRoom(@PathVariable("id") Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Optional<Room> roomOptional = roomRepository.findById(id);
        if (roomOptional.isPresent()) {
            Room room = roomOptional.get();
            if (room.getLandlordEmail() != null && room.getLandlordEmail().equalsIgnoreCase(loggedInUser.getEmail())) {
                roomRepository.deleteById(id);
            }
        }
        return "redirect:/home?roomDeleted=true";
    }

    // ==========================================
    // MALIPO NA BOOKING (REKEBISHO LILILOFANYIKA HAPA)
    // ==========================================

    @PostMapping("/pay-room")
    public String payForRoom(@RequestParam("roomId") Long roomId, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // 1. Omba Auth Token kutoka Pesapal
            String token = pesapalService.getAuthToken();
            if (token == null) {
                return "redirect:/home?error=pesapal_auth_failed";
            }

            // 2. Kutengeneza Reference ya kipekee
            String merchantRef = "PS-" + UUID.randomUUID().toString().substring(0, 8);
            Double amount = 1000.00;

            // 3. Omba redirect URL kutoka Pesapal
            String redirectUrl = pesapalService.submitOrder(token, merchantRef, amount, user.getEmail(), user.getPhone());

            if (redirectUrl != null) {
                // 4. Hifadhi PENDING Payment kwenye Database
                Payment payment = new Payment();
                payment.setUserId(user.getId());
                payment.setRoomId(roomId);
                payment.setAmount(amount);
                payment.setMerchantReference(merchantRef);
                payment.setStatus("PENDING"); // Hatusave COMPLETED tena hapa!
                paymentRepository.save(payment);

                // 5. Elekeza mtumiaji kwenda Pesapal kulipa
                return "redirect:" + redirectUrl;
            }
        } catch (Exception e) {
            System.err.println("Pay Room Error: " + e.getMessage());
        }

        return "redirect:/home?error=payment_failed";
    }

    @PostMapping("/book-room")
    public String bookRoom(@RequestParam Long roomId,
                           @RequestParam String roomTitle,
                           @RequestParam String tenantPhone) {

        Booking booking = new Booking();
        booking.setRoomId(roomId);
        booking.setRoomTitle(roomTitle);
        booking.setTenantPhone(tenantPhone);
        booking.setStatus("PENDING");

        roomRepository.findById(roomId).ifPresent(room -> {
            booking.setLandlordEmail(room.getLandlordEmail());
        });

        bookingRepository.save(booking);

        try {
            String msg = "PangaSmart: Ombi lako la chumba '" + roomTitle + "' limepokelewa. Mwenye nyumba atakujibu hivi karibuni.";
            smsService.sendSms(tenantPhone, msg);
        } catch (Exception e) {
            System.out.println("SMS error: " + e.getMessage());
        }

        return "redirect:/home?booked=true";
    }

    @PostMapping("/approve-booking")
    public String approveBooking(@RequestParam Long bookingId) {
        bookingRepository.findById(bookingId).ifPresent(b -> {
            b.setStatus("APPROVED");
            bookingRepository.save(b);

            try {
                String msg = "PangaSmart: Hongera! Ombi lako la chumba '" + b.getRoomTitle() + "' LIMEKUBALIWA na mwenye nyumba.";
                smsService.sendSms(b.getTenantPhone(), msg);
            } catch (Exception e) {
                System.out.println("SMS error: " + e.getMessage());
            }
        });
        return "redirect:/home";
    }

    @PostMapping("/reject-booking")
    public String rejectBooking(@RequestParam Long bookingId) {
        bookingRepository.findById(bookingId).ifPresent(b -> {
            b.setStatus("REJECTED");
            bookingRepository.save(b);

            try {
                String msg = "PangaSmart: Samahani, ombi lako la chumba '" + b.getRoomTitle() + "' LIMEKATALIWA.";
                smsService.sendSms(b.getTenantPhone(), msg);
            } catch (Exception e) {
                System.out.println("SMS error: " + e.getMessage());
            }
        });
        return "redirect:/home";
    }
}
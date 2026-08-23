package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.models.Booking;
import com.pangasmart.pangasmart.models.Room;
import com.pangasmart.pangasmart.models.User;
import com.pangasmart.pangasmart.repositories.BookingRepository;
import com.pangasmart.pangasmart.repositories.RoomRepository;
import com.pangasmart.pangasmart.services.SmsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class HomeController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SmsService smsService;

    private static final String UPLOAD_DIR = "uploads/";

    @GetMapping("/home")
    public String showHomePage(Model model,
                               HttpSession session,
                               @RequestParam(value = "search", required = false) String search) {

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

        // Weka email ya Landlord aliyelog-in
        room.setLandlordEmail(user.getEmail());

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Hifadhi Picha kama ipo
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                Files.copy(imageFile.getInputStream(), uploadPath.resolve(imageName), StandardCopyOption.REPLACE_EXISTING);
                room.setImageUrl("/uploads/" + imageName);
            }

            // Hifadhi Video kama ipo
            if (videoFile != null && !videoFile.isEmpty()) {
                String videoName = UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
                Files.copy(videoFile.getInputStream(), uploadPath.resolve(videoName), StandardCopyOption.REPLACE_EXISTING);
                room.setVideoUrl("/uploads/" + videoName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save Room kwenye Database
        roomRepository.save(room);
        return "redirect:/home?roomAdded=true";
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
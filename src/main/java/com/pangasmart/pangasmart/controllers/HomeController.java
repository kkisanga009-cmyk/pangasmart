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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired(required = false)
    private SmsService smsService;

    @Autowired(required = false)
    private PesapalService pesapalService;

    @GetMapping("/home")
    public String showHomePage(Model model,
                               HttpSession session,
                               @RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "paidRoomId", required = false) Long paidRoomId,
                               @RequestParam(value = "page", defaultValue = "0") int page) {
        try {
            User user = (User) session.getAttribute("loggedInUser");

            if (user == null) {
                return "redirect:/login";
            }

            String userRole = (String) session.getAttribute("userRole");
            if (userRole == null && user.getRole() != null) {
                userRole = user.getRole();
                session.setAttribute("userRole", userRole);
            }

            User landlord = null;
            List<Booking> landlordBookings = new ArrayList<>();
            boolean isBookingAllowedByAdmin = false;

            if ("LANDLORD".equalsIgnoreCase(userRole)) {
                Optional<User> landlordOpt = userRepository.findByEmail(user.getEmail());
                if (landlordOpt.isPresent()) {
                    landlord = landlordOpt.get();
                    model.addAttribute("landlord", landlord);
                    model.addAttribute("adminMessage", landlord.getAdminMessage());

                    boolean isApprovedByAdmin = "APPROVED".equalsIgnoreCase(landlord.getStatus());
                    isBookingAllowedByAdmin = landlord.isAllowBooking() && isApprovedByAdmin;
                }

                if (isBookingAllowedByAdmin) {
                    List<Room> myRooms = roomRepository.findAllByLandlordEmail(user.getEmail());
                    List<Booking> allBookings = bookingRepository.findAll();

                    if (allBookings != null) {
                        for (Booking b : allBookings) {
                            boolean matchesEmail = (b.getLandlordEmail() != null && b.getLandlordEmail().equalsIgnoreCase(user.getEmail()));
                            boolean matchesRoom = false;

                            if (myRooms != null && b.getRoomId() != null) {
                                for (Room r : myRooms) {
                                    if (r.getId() != null && r.getId().equals(b.getRoomId())) {
                                        matchesRoom = true;
                                        break;
                                    }
                                }
                            }

                            if (matchesEmail || matchesRoom) {
                                landlordBookings.add(b);
                            }
                        }
                    }
                }
            }

            Pageable pageable = PageRequest.of(page, 6);
            Page<Room> roomPage;

            if ("LANDLORD".equalsIgnoreCase(userRole)) {
                if (search != null && !search.trim().isEmpty()) {
                    roomPage = roomRepository.searchLandlordRooms(user.getEmail(), search, pageable);
                } else {
                    roomPage = roomRepository.findByLandlordEmail(user.getEmail(), pageable);
                }
            } else {
                if (search != null && !search.trim().isEmpty()) {
                    roomPage = roomRepository.findByTitleContainingIgnoreCaseOrLocationContainingIgnoreCase(search, search, pageable);
                } else {
                    roomPage = roomRepository.findAll(pageable);
                }
            }

            for (Room room : roomPage.getContent()) {
                if (room.getLandlordEmail() != null) {
                    Optional<User> ownerOpt = userRepository.findByEmail(room.getLandlordEmail());
                    if (ownerOpt.isPresent()) {
                        User owner = ownerOpt.get();
                        boolean isApprovedByAdmin = "APPROVED".equalsIgnoreCase(owner.getStatus());
                        room.setAllowBooking(owner.isAllowBooking() && isApprovedByAdmin);

                        room.setBookingFee(owner.getBookingFee());

                        if (room.getLandlordName() == null || room.getLandlordName().isEmpty()) {
                            room.setLandlordName(owner.getFullName());
                        }
                    } else {
                        room.setAllowBooking(false);
                    }
                }
            }

            List<Long> paidRoomIds = new ArrayList<>();
            try {
                List<Payment> userCompletedPayments = paymentRepository.findByUserIdAndStatus(user.getId(), "COMPLETED");
                if (userCompletedPayments != null) {
                    for (Payment p : userCompletedPayments) {
                        if (p.getRoomId() != null) {
                            paidRoomIds.add(p.getRoomId());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching completed payments: " + e.getMessage());
            }

            if (paidRoomId != null) {
                try {
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
                } catch (Exception e) {
                    System.err.println("Error processing paidRoomId: " + e.getMessage());
                }
            }

            model.addAttribute("currentUser", user);
            model.addAttribute("userRole", userRole != null ? userRole : "TENANT");
            model.addAttribute("rooms", roomPage.getContent());
            model.addAttribute("roomPage", roomPage);
            model.addAttribute("bookings", isBookingAllowedByAdmin ? landlordBookings : null);
            model.addAttribute("searchKeyword", search);
            model.addAttribute("paidRoomIds", paidRoomIds);

            return "home";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login?error=session_expired";
        }
    }

    @PostMapping("/landlord/booking-settings")
    public String updateBookingSettings(@RequestParam(value = "allowBooking", required = false) Boolean allowBooking,
                                        @RequestParam(value = "bookingFee", required = false) Double bookingFee,
                                        HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !"LANDLORD".equalsIgnoreCase(user.getRole())) {
            return "redirect:/login";
        }

        try {
            Optional<User> userOpt = userRepository.findByEmail(user.getEmail());
            if (userOpt.isPresent()) {
                User landlord = userOpt.get();
                boolean wantsBooking = allowBooking != null && allowBooking;

                landlord.setAllowBooking(wantsBooking);
                landlord.setBookingFee(bookingFee != null ? bookingFee : 0.0);

                if (wantsBooking) {
                    landlord.setBookingRequestStatus("PENDING");
                } else {
                    landlord.setBookingRequestStatus("INACTIVE");
                }

                userRepository.save(landlord);
                session.setAttribute("loggedInUser", landlord);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/home?error=settings_failed";
        }

        return "redirect:/home?bookingSettingsUpdated=true";
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

        try {
            room.setLandlordEmail(user.getEmail());
            room.setLandlordName(user.getFullName());

            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                room.setLandlordPhone(user.getPhone());
            }

            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                String imgFileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Files.copy(imageFile.getInputStream(), uploadPath.resolve(imgFileName), StandardCopyOption.REPLACE_EXISTING);
                room.setImageUrl("/uploads/" + imgFileName);
            }

            if (videoFile != null && !videoFile.isEmpty()) {
                String videoFileName = System.currentTimeMillis() + "_" + videoFile.getOriginalFilename();
                Files.copy(videoFile.getInputStream(), uploadPath.resolve(videoFileName), StandardCopyOption.REPLACE_EXISTING);
                room.setVideoUrl("/uploads/" + videoFileName);
            }

            roomRepository.save(room);
            return "redirect:/home?roomAdded=true";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/home?error=room_add_failed";
        }
    }

    @GetMapping("/landlord/rooms/delete-image/{id}")
    public String deleteRoomImage(@PathVariable("id") Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        try {
            Optional<Room> roomOptional = roomRepository.findById(id);
            if (roomOptional.isPresent()) {
                Room room = roomOptional.get();
                if (room.getLandlordEmail() != null && room.getLandlordEmail().equalsIgnoreCase(loggedInUser.getEmail())) {
                    room.setImageUrl(null);
                    roomRepository.save(room);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/home?imageDeleted=true";
    }

    @GetMapping("/landlord/rooms/delete-video/{id}")
    public String deleteRoomVideo(@PathVariable("id") Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        try {
            Optional<Room> roomOptional = roomRepository.findById(id);
            if (roomOptional.isPresent()) {
                Room room = roomOptional.get();
                if (room.getLandlordEmail() != null && room.getLandlordEmail().equalsIgnoreCase(loggedInUser.getEmail())) {
                    room.setVideoUrl(null);
                    roomRepository.save(room);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/home?videoDeleted=true";
    }

    @GetMapping("/landlord/rooms/delete/{id}")
    public String deleteRoom(@PathVariable("id") Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        try {
            Optional<Room> roomOptional = roomRepository.findById(id);
            if (roomOptional.isPresent()) {
                Room room = roomOptional.get();
                if (room.getLandlordEmail() != null && room.getLandlordEmail().equalsIgnoreCase(loggedInUser.getEmail())) {
                    roomRepository.deleteById(id);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/home?roomDeleted=true";
    }

    @PostMapping("/pay-room")
    public String payForRoom(@RequestParam("roomId") Long roomId, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            if (pesapalService == null) {
                return "redirect:/home?error=payment_service_unavailable";
            }

            String token = pesapalService.getAuthToken();
            if (token == null) {
                return "redirect:/home?error=pesapal_auth_failed";
            }

            String merchantRef = "PS-" + UUID.randomUUID().toString().substring(0, 8);
            Double amount = 1000.00;

            String redirectUrl = pesapalService.submitOrder(token, merchantRef, amount, user.getEmail(), user.getPhone());

            if (redirectUrl != null) {
                Payment payment = new Payment();
                payment.setUserId(user.getId());
                payment.setRoomId(roomId);
                payment.setAmount(amount);
                payment.setMerchantReference(merchantRef);
                payment.setStatus("PENDING");
                paymentRepository.save(payment);

                return "redirect:" + redirectUrl;
            }
        } catch (Exception e) {
            System.err.println("Pay Room Error: " + e.getMessage());
        }

        return "redirect:/home?error=payment_failed";
    }

    @PostMapping("/tenant/book-room")
    public String bookRoom(@RequestParam("roomId") Long roomId, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();

                double fee = 0.0;
                Optional<User> landlordOpt = userRepository.findByEmail(room.getLandlordEmail());
                if (landlordOpt.isPresent()) {
                    fee = landlordOpt.get().getBookingFee();
                }

                Booking booking = new Booking();
                booking.setTenantId(user.getId());
                booking.setTenantName(user.getFullName());
                booking.setTenantEmail(user.getEmail());
                booking.setTenantPhone(user.getPhone());

                booking.setRoomId(roomId);
                booking.setRoomTitle(room.getTitle());
                booking.setBookingAmount(fee);

                booking.setLandlordId(landlordOpt.map(User::getId).orElse(null));
                booking.setLandlordName(room.getLandlordName());
                booking.setLandlordEmail(room.getLandlordEmail());
                booking.setLandlordPhone(room.getLandlordPhone());

                booking.setStatus("PENDING");
                booking.setCreatedAt(LocalDateTime.now());

                bookingRepository.save(booking);

                return "redirect:/home?bookingSubmitted=true";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/home?error=booking_failed";
    }
}
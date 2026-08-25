package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.models.User;
import com.pangasmart.pangasmart.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String showLandingPage() {
        return "index";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        if ("LANDLORD".equalsIgnoreCase(user.getRole())) {
            user.setStatus("PENDING");
            userRepository.save(user);

            String whatsappUrl = "https://wa.me/255747466962";
            try {
                String fullName = (user.getFullName() != null) ? user.getFullName() : "";
                String email = (user.getEmail() != null) ? user.getEmail() : "";
                String message = "Habari Admin, nimejisajili kama Landlord kwenye PangaSmart. Naomba kibali cha akaunti yangu. Jina: "
                        + fullName + ", Email: " + email;
                String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name());
                whatsappUrl = "https://wa.me/255747466962?text=" + encodedMessage;
            } catch (Exception e) {
                System.err.println("Encoding error: " + e.getMessage());
            }

            redirectAttributes.addFlashAttribute("landlordName", user.getFullName());
            redirectAttributes.addFlashAttribute("whatsappUrl", whatsappUrl);

            return "redirect:/pending-approval";
        } else {
            user.setStatus("APPROVED");
            userRepository.save(user);
            return "redirect:/login?success";
        }
    }

    @GetMapping("/pending-approval")
    public String showPendingApprovalPage(Model model) {
        if (!model.containsAttribute("whatsappUrl")) {
            try {
                String defaultMessage = "Habari Admin, naomba kibali cha akaunti yangu ya Landlord kwenye PangaSmart.";
                String encodedMessage = URLEncoder.encode(defaultMessage, StandardCharsets.UTF_8.name());
                model.addAttribute("whatsappUrl", "https://wa.me/255747466962?text=" + encodedMessage);
            } catch (Exception e) {
                model.addAttribute("whatsappUrl", "https://wa.me/255747466962");
            }
        }
        if (!model.containsAttribute("landlordName")) {
            model.addAttribute("landlordName", "Mwenye Nyumba");
        }
        return "pending-approval";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam("email") String email,
                              @RequestParam("password") String password,
                              HttpSession session,
                              Model model) {

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null && user.getPassword().equals(password)) {

            if ("LANDLORD".equalsIgnoreCase(user.getRole())) {
                if ("PENDING".equalsIgnoreCase(user.getStatus())) {
                    String whatsappUrl = "https://wa.me/255747466962";
                    try {
                        String message = "Habari Admin, naomba kibali cha akaunti yangu ya Landlord kwenye PangaSmart. Email: " + user.getEmail();
                        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name());
                        whatsappUrl = "https://wa.me/255747466962?text=" + encodedMessage;
                    } catch (Exception e) {
                        System.err.println("Encoding error: " + e.getMessage());
                    }

                    model.addAttribute("errorPending", true);
                    model.addAttribute("whatsappUrl", whatsappUrl);
                    return "login";
                } else if ("REJECTED".equalsIgnoreCase(user.getStatus())) {
                    model.addAttribute("errorRejected", "Hujakidhi vigezo, hivyo huwezi kujisajili wala kuingia kama Landlord.");
                    return "login";
                }
            }

            session.setAttribute("loggedInUser", user);
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userRole", user.getRole());

            return "redirect:/home";
        }

        return "redirect:/login?error=InvalidCredentials";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            model.addAttribute("email", email);
            return "reset-password";
        } else {
            model.addAttribute("error", "⚠️ Barua pepe hii haijasajiliwa kwenye mfumo!");
            return "forgot-password";
        }
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("email") String email,
                                       @RequestParam("newPassword") String newPassword,
                                       Model model) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPassword(newPassword);
            userRepository.save(user);

            model.addAttribute("message", "✅ Nenosiri limebadilishwa kikamilifu! Ingia sasa.");
            return "login";
        } else {
            model.addAttribute("error", "⚠️ Imeshindikana kubadili nenosiri. Jaribu tena.");
            return "forgot-password";
        }
    }

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", loggedInUser);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute User updatedUser, HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        loggedInUser.setFullName(updatedUser.getFullName());
        loggedInUser.setPhone(updatedUser.getPhone());

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
            loggedInUser.setPassword(updatedUser.getPassword());
        }

        userRepository.save(loggedInUser);

        session.setAttribute("loggedInUser", loggedInUser);
        model.addAttribute("message", "✅ Taarifa zako zimesasishwa kikamilifu!");
        model.addAttribute("user", loggedInUser);

        return "profile";
    }
}
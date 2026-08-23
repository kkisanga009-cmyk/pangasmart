package com.pangasmart.pangasmart.controllers;

import com.pangasmart.pangasmart.models.User;
import com.pangasmart.pangasmart.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String registerUser(@ModelAttribute User user) {
        userRepository.save(user);
        return "redirect:/login?success";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam("email") String email,
                              @RequestParam("password") String password,
                              HttpSession session,
                              Model model) {

        // 1. Tafuta mtumiaji kwenye Database
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null && user.getPassword().equals(password)) {

            // 2. Weka data muhimu kwenye Session (MUHIMU SANA)
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userRole", user.getRole()); // Mfano: 'LANDLORD', 'TENANT', 'ADMIN'

            return "redirect:/home";
        }

        return "redirect:/login?error=InvalidCredentials";
    }

    // --- PASSWORD RESET ENDPOINTS ---

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

    // --- USER PROFILE ENDPOINTS ---

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
package com.urbix.controller;

import com.urbix.dto.CustomUserDetails;
import com.urbix.dto.UserDTO;
import com.urbix.entity.User;
import com.urbix.repository.UserRepository;
import com.urbix.service.OTPService;
import com.urbix.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/otp")
public class AuthController {
    public static final String REDIRECT_LOGIN = "redirect:/login";
    public static final String SUCCESS_MESSAGE = "successMessage";
    public static final String REDIRECT_REGISTER = "redirect:/register";
    public static final String USER_DTO = "userDTO";
    public static final String ERROR_MESSAGE = "errorMessage";
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OTPService otpService;
    @Autowired
    private UserService userService;

    @GetMapping("/verify-register")
    public String showVerifyRegister(){
        return "verify-register";
    }
    @PostMapping("/user-login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("User LOGGING in = " + email);
            User user = userService.authenticateUser(email, password);

            if (user != null) {
                session.setAttribute("userId", user.getId());
                session.setAttribute("loggedInUser", user);
                System.out.println("The user type = "+user.getUserType());
                session.setAttribute("userType", user.getUserType());
                session.setAttribute("userIsThere", true);
                // Convert User to CustomUserDetails
                CustomUserDetails userDetails = new CustomUserDetails(user);

                // Manually set authentication
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // ✅ Explicitly store SecurityContext in session
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

                return "redirect:/dashboard";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid email or password");
                return "redirect:/login?error";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred during login");
            return "redirect:/login?error";
        }
    }



    // Send OTP for Registration
    @PostMapping("/send-otp")
    public String sendOTP(@ModelAttribute UserDTO userDTO, HttpSession session,
                          RedirectAttributes redirectAttributes, Model model) {
        try {
            if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "This Email Is Already Taken. Please Login!");
                return REDIRECT_LOGIN;
            }
            System.out.println("EMAIL from send OTP = "+userDTO.getEmail());
            // Generate and send OTP (store in DB)

            String otp = otpService.generateOTP(userDTO.getEmail());
            otpService.sendOTP(userDTO.getEmail(), otp);
            session.setAttribute(USER_DTO,userDTO);
            // Redirect to OTP verification page
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "OTP sent to email. Please verify.");
            return "redirect:/api/otp/verify-register";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Error sending OTP. Please try again."+e.getMessage());
            return REDIRECT_REGISTER;
        }
    }

    @PostMapping("/verify-otp")
    @Transactional
    public String verifyOTP(@RequestParam String email,
                            @RequestParam String otp,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            // ✅ Retrieve user data from session
            UserDTO userDTO = (UserDTO) session.getAttribute(USER_DTO);

            if (userDTO == null || !userDTO.getEmail().equals(email)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Session expired. Please register again.");
                return REDIRECT_REGISTER;
            }

            System.out.println("VERIFY OTP Controller");
            System.out.println("Email verifying is = " + userDTO.getEmail());
            System.out.println("Entered OTP = " + otp);

            if (!otpService.verifyOTP(userDTO.getEmail(), otp)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Invalid OTP. Try again!");
                return "redirect:/api/otp/verify-register";
            }

            // ✅ Save user to database
            User newUser = new User();
            newUser.setEmail(userDTO.getEmail());
            newUser.setProvider("URBIX");
            newUser.setPassword(userDTO.getPassword());
            newUser.setUserType(userDTO.getUserType());
            userRepository.save(newUser);

            // ✅ Clear session after successful registration
            session.removeAttribute(USER_DTO);

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "User registered successfully! Please login.");
            return REDIRECT_LOGIN;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getLocalizedMessage());
            return "redirect:/login";
        }
    }


    @GetMapping("/resend-otp")
    public String resendOTP(@RequestParam String email, RedirectAttributes redirectAttributes) {
        if (otpService.resendOTP(email)) {
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "A new OTP has been sent to your email.");
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Your current OTP is still valid. Please use it.");
        }
        return "redirect:/api/otp/verify-register?email=" + email;
    }

}
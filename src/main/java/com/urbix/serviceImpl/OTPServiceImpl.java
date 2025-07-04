package com.urbix.serviceImpl;

import com.urbix.entity.OTP;
import com.urbix.repository.OTPRepository;
import com.urbix.service.OTPService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import java.util.Random;

@Service
public class OTPServiceImpl implements OTPService {


    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private OTPRepository otpRepository;

    @Override
    public String generateOTP(String email) {
        String otp = String.format("%06d", new Random().nextInt(1000000));
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(5);

        // Remove old OTP if exists
        otpRepository.findByEmail(email).ifPresent(otpRepository::delete);

        // Save new OTP in DB
        OTP newOtp = new OTP(email, otp, expirationTime);
        otpRepository.save(newOtp);
        return otp;
    }
    @Override
    public boolean resendOTP(String email) {
        OTP otpEntry = otpRepository.findByEmail(email).orElse(null);

        if (otpEntry != null && LocalDateTime.now().isBefore(otpEntry.getExpirationTime())) {
            return false; // Existing OTP is still valid
        }

        String newOtp = generateOTP(email);
        sendOTP(email, newOtp);
        return true;
    }

    @Override
    public void sendOTP(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP code is: " + otp + "\nThis OTP is valid for 5 minutes.");
        mailSender.send(message);
    }

    @Override
    public boolean verifyOTP(String email, String otp) {
        System.out.println("INTHE OTP Service layer");
        OTP otpEntry = otpRepository.findByEmail(email).orElse(null);
        if (otpEntry == null || LocalDateTime.now().isAfter(otpEntry.getExpirationTime())) {
            System.out.println("Expired OTP or NULL");
            return false; // Expired or not found
        }
        if (otpEntry.getOtpCode().equals(otp)) {
            otpRepository.delete(otpEntry); // OTP should be one-time use
            return true;
        }
        return false;
    }
}

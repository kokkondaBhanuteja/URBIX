package com.urbix.service;

public interface OTPService {
    String generateOTP(String email);
    void sendOTP(String email, String otp);
    boolean verifyOTP(String email, String otp);

    boolean resendOTP(String email);

}

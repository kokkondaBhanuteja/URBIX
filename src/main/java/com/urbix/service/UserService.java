package com.urbix.service;

import com.urbix.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    User authenticateUser(String email, String password);
}

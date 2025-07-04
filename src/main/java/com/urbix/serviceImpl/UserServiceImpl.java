package com.urbix.serviceImpl;

import com.urbix.entity.User;
import com.urbix.repository.UserRepository;
import com.urbix.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserRepository userRepository;

    @Override
    public User authenticateUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (password.equals(user.getPassword())) {
                return user;
            }
        }
        return null;
    }
}

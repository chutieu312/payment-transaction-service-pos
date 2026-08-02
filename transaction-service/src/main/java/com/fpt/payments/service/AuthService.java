package com.fpt.payments.service;

import com.fpt.payments.dto.LoginRequest;
import com.fpt.payments.dto.RegisterRequest;
import com.fpt.payments.entity.User;
import com.fpt.payments.exception.ConflictException;
import com.fpt.payments.repository.UserRepository;
import com.fpt.payments.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, String> login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return Map.of("token", token, "role", user.getRole(), "email", user.getEmail());
    }

    public Map<String, String> register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered");
        }
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(req.role().toUpperCase())
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return Map.of("token", token, "role", user.getRole(), "email", user.getEmail());
    }
}

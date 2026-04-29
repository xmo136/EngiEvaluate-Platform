package com.example.assessment.controller;

import com.example.assessment.dto.LoginRequest;
import com.example.assessment.dto.LoginResponse;
import com.example.assessment.dto.PasswordChangeRequest;
import com.example.assessment.service.AuthService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/change-password")
    public LoginResponse changePassword(@RequestHeader(value = "X-Username", required = false) String username,
                                        @RequestHeader(value = "X-User-Role", required = false) String role,
                                        @RequestBody PasswordChangeRequest request) {
        return authService.changePassword(username, role, request);
    }

    @GetMapping("/demo-accounts")
    public List<Map<String, String>> demoAccounts() {
        return List.of(
                Map.of("role", "管理员", "username", "admin", "password", "123456"),
                Map.of("role", "教师", "username", "teacher", "password", "123456"),
                Map.of("role", "学生", "username", "student", "password", "123456"),
                Map.of("role", "学生", "username", "student2", "password", "123456")
        );
    }
}

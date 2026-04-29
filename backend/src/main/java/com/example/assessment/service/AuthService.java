package com.example.assessment.service;

import com.example.assessment.dto.LoginRequest;
import com.example.assessment.dto.LoginResponse;
import com.example.assessment.dto.PasswordChangeRequest;
import com.example.assessment.model.UserRole;
import com.example.assessment.persistence.entity.UserAccountEntity;
import com.example.assessment.persistence.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;

    public AuthService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public LoginResponse login(LoginRequest request) {
        UserAccountEntity account = userAccountRepository.findByUsername(request.username())
                .orElse(null);
        if (account == null || !Objects.equals(account.getPassword(), request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return toResponse(account);
    }

    public UserAccountEntity requireAnyRole(String usernameHeader, String roleHeader, UserRole... allowedRoles) {
        UserRole actualRole = parseRole(roleHeader);
        UserAccountEntity account = loadAccount(usernameHeader, actualRole);
        for (UserRole allowedRole : allowedRoles) {
            if (actualRole == allowedRole) {
                if (account.isPasswordChangeRequired()) {
                    throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Password change required");
                }
                return account;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current role is not allowed");
    }

    public UserAccountEntity currentAccount(String usernameHeader, String roleHeader) {
        UserRole actualRole = parseRole(roleHeader);
        return loadAccount(usernameHeader, actualRole);
    }

    @Transactional
    public LoginResponse changePassword(String usernameHeader, String roleHeader, PasswordChangeRequest request) {
        UserAccountEntity account = currentAccount(usernameHeader, roleHeader);
        if (request == null || request.currentPassword() == null || request.newPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password payload is required");
        }
        if (!Objects.equals(account.getPassword(), request.currentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        String nextPassword = request.newPassword().trim();
        if (nextPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters");
        }
        if (Objects.equals(account.getPassword(), nextPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different");
        }

        account.setPassword(nextPassword);
        account.setPasswordChangeRequired(false);
        return toResponse(userAccountRepository.save(account));
    }

    private UserRole parseRole(String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login first");
        }
        try {
            return UserRole.valueOf(roleHeader);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid role");
        }
    }

    private UserAccountEntity loadAccount(String usernameHeader, UserRole actualRole) {
        if (usernameHeader == null || usernameHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing username");
        }
        String username = decodeUsernameHeader(usernameHeader);
        UserAccountEntity account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        if (account.getRole() != actualRole) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session role does not match account");
        }
        return account;
    }

    private String decodeUsernameHeader(String usernameHeader) {
        try {
            return URLDecoder.decode(usernameHeader, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return usernameHeader;
        }
    }

    private LoginResponse toResponse(UserAccountEntity account) {
        return new LoginResponse(
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getRole(),
                account.getRole().getLabel(),
                account.getStudent() == null ? null : account.getStudent().getId(),
                allowedViews(account.getRole()),
                account.isPasswordChangeRequired()
        );
    }

    private List<String> allowedViews(UserRole role) {
        return switch (role) {
            case ADMIN -> List.of("dashboard", "teaching", "students", "questions", "reports");
            case TEACHER -> List.of("dashboard", "students", "questions", "exam", "results", "reports");
            case STUDENT -> List.of("exam");
        };
    }
}

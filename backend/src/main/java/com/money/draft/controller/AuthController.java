
package com.money.draft.controller;

import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.CaptchaChallenge;
import com.money.draft.dto.ErrorResponse;
import com.money.draft.dto.LoginRequest;
import com.money.draft.dto.LoginResponse;
import com.money.draft.security.JwtService;
import com.money.draft.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "Auth", description = "Authentication: login for user/admin")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AppUserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final CaptchaService captchaService;

    public AuthController(AppUserRepository userRepo, PasswordEncoder encoder, JwtService jwt, CaptchaService captchaService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.captchaService = captchaService;
    }

    @Operation(summary = "Generate a CAPTCHA challenge")
    @GetMapping("/captcha")
    public ResponseEntity<CaptchaChallenge> captcha() {
        return ResponseEntity.ok(captchaService.generateChallenge());
    }

    @Operation(summary = "Login with username and password")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        if (!captchaService.validateCaptcha(req.captchaToken(), req.captchaAnswer())) {
            ErrorResponse body = new ErrorResponse(
                    "INVALID_CAPTCHA",
                    "Invalid or expired captcha. Please refresh and try again.",
                    httpReq.getRequestURI(),
                    Instant.now()
            );
            return ResponseEntity.status(400).body(body);
        }
        var user = userRepo.findByUsername(req.username()).orElse(null);

        if (user == null || !encoder.matches(req.password(), user.getPassword())) {
            // Consistent error payload with the rest of the API
            ErrorResponse body = new ErrorResponse(
                    "UNAUTHORIZED",
                    "Invalid username or password",
                    httpReq.getRequestURI(),
                    Instant.now()
            );
            return ResponseEntity.status(401).body(body);
        }

        String token = jwt.generateToken(user.getUsername(), user.getRole().name(), user.getAccountId());
        return ResponseEntity.ok(new LoginResponse(token, user.getRole().name(), user.getAccountId()));
    }
}

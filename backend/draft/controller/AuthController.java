
package com.money.draft.controller;

import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.LoginRequest;   // record LoginRequest(String username, String password) {}
import com.money.draft.dto.LoginResponse;
import com.money.draft.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AppUserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(AppUserRepository userRepo, PasswordEncoder encoder, JwtService jwt) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        var user = userRepo.findByUsername(req.username()).orElse(null);

        if (user == null || !encoder.matches(req.password(), user.getPassword())) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("status", 401);
            body.put("error", "UNAUTHORIZED");
            body.put("message", "Invalid username or password");
            body.put("path", "/auth/login");
            return ResponseEntity.status(401).body(body);
        }

        String token = jwt.generateToken(user.getUsername(), user.getRole().name(), user.getAccountId());
        return ResponseEntity.ok(new LoginResponse(token, user.getRole().name(), user.getAccountId()));
    }
}

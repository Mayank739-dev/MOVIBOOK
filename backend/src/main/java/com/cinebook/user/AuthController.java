package com.cinebook.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "").trim();
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String password = body.getOrDefault("password", "").trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Name, email, and password are required."
            ));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Password must be at least 6 characters long."
            ));
        }

        if (userRepo.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "An account with this email address already exists."
            ));
        }

        User user = new User(name, email, passwordEncoder.encode(password));
        userRepo.save(user);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Account registered successfully!",
            "user", Map.of(
                "id", user.getId().toString(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole()
            )
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String password = body.getOrDefault("password", "").trim();

        if (email.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Email and password are required."
            ));
        }

        Optional<User> opt = userRepo.findByEmailIgnoreCase(email);
        if (opt.isEmpty() || !passwordEncoder.matches(password, opt.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid email address or password. Please try again."
            ));
        }

        User user = opt.get();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Login successful!",
            "user", Map.of(
                "id", user.getId().toString(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole()
            )
        ));
    }

    @GetMapping("/demo-users")
    public List<Map<String, String>> getDemoUsers() {
        return List.of(
            Map.of("name", "Alex Johnson", "email", "alex@cinebook.com", "password", "password123", "role", "Verified Member"),
            Map.of("name", "Sarah Miller", "email", "sarah@cinebook.com", "password", "password123", "role", "IMAX Enthusiast"),
            Map.of("name", "Rahul Sharma", "email", "rahul@cinebook.com", "password", "password123", "role", "Cinema Club Pro")
        );
    }
}


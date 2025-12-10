package com.bankinc.prueba.controller;

import com.bankinc.prueba.dto.AuthRequest;
import com.bankinc.prueba.dto.AuthResponse;
import com.bankinc.prueba.dto.RegisterRequest;
import com.bankinc.prueba.model.User;
import com.bankinc.prueba.security.JwtTokenProvider;
import com.bankinc.prueba.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService authService, JwtTokenProvider tokenProvider, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        User u = authService.register(req.getUsername(), req.getPassword());
        String token = tokenProvider.createToken(u.getUsername());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
            String token = tokenProvider.createToken(req.getUsername());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid username/password");
        }
    }
}

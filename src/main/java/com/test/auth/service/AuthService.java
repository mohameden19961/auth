package com.test.auth.service;

import com.test.auth.dto.*;
import com.test.auth.entity.User;
import com.test.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final UserDetailsServiceImpl userDetailsService;

    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Cet email est déjà utilisé");

        String code = String.format("%06d", new Random().nextInt(999999));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .verificationCode(code)
                .verificationExpiry(LocalDateTime.now().plusMinutes(15))
                .build();

        userRepository.save(user);
        emailService.sendVerificationEmail(request.getEmail(), request.getFirstName(), code);
        return new MessageResponse("Inscription réussie ! Vérifiez votre email.");
    }

    public MessageResponse verifyEmail(String code) {
        User user = userRepository.findByVerificationCode(code)
                .orElseThrow(() -> new RuntimeException("Code invalide"));

        if (LocalDateTime.now().isAfter(user.getVerificationExpiry()))
            throw new RuntimeException("Code expiré.");

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationExpiry(null);
        userRepository.save(user);
        return new MessageResponse("Email vérifié ! Vous pouvez vous connecter.");
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        if (!user.isEnabled())
            throw new RuntimeException("Compte non vérifié.");

        var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token).email(user.getEmail())
                .firstName(user.getFirstName()).lastName(user.getLastName())
                .message("Connexion réussie !")
                .build();
    }
}

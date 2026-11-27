package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.*;
import com.fooddelivery.dto.response.AuthResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.security.*;
import com.fooddelivery.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final DeliveryPartnerRepository deliveryPartnerRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService uds;

    @Value("${app.jwt.refresh-token-expiration}") private long refreshExpiry;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new DuplicateException("Email already registered");
        if (userRepo.existsByPhone(req.getPhone()))
            throw new DuplicateException("Phone number already registered");

        String roleName = req.getRole().toUpperCase(Locale.ROOT);
        if (roleName.equals("ADMIN"))
            throw new BusinessException("Cannot self-register as ADMIN");

        Role role = roleRepo.findByName(roleName)
                .orElseThrow(() -> new BusinessException("Invalid role: " + req.getRole()));

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .active(true)
                .build();
        userRepo.save(user);

        if (roleName.equals("DELIVERY_PARTNER")) {
            deliveryPartnerRepo.save(DeliveryPartner.builder().user(user)
                    .available(false).totalDeliveries(0)
                    .totalEarnings(java.math.BigDecimal.ZERO).build());
        }

        UserDetails ud = uds.loadUserByUsername(user.getEmail());
        String accessToken  = jwtUtil.generateToken(ud, user.getId(), roleName);
        String refreshToken = createRefreshToken(user);

        return buildResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));

        if (!user.isActive())
            throw new BusinessException("Account is deactivated. Contact support.");

        refreshTokenRepo.revokeAllByUser(user);

        UserDetails ud = uds.loadUserByUsername(user.getEmail());
        String accessToken  = jwtUtil.generateToken(ud, user.getId(), user.getRole().getName());
        String refreshToken = createRefreshToken(user);

        return buildResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest req) {
        RefreshToken rt = refreshTokenRepo.findByToken(req.getRefreshToken())
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (rt.isRevoked())
            throw new BusinessException("Refresh token has been revoked");
        if (rt.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new BusinessException("Refresh token has expired");

        User user = rt.getUser();
        rt.setRevoked(true);
        refreshTokenRepo.save(rt);

        UserDetails ud = uds.loadUserByUsername(user.getEmail());
        String accessToken     = jwtUtil.generateToken(ud, user.getId(), user.getRole().getName());
        String newRefreshToken = createRefreshToken(user);

        return buildResponse(user, accessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepo.findByToken(refreshToken).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepo.save(rt);
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String createRefreshToken(User user) {
        RefreshToken rt = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiry / 1000))
                .revoked(false)
                .build();
        return refreshTokenRepo.save(rt).getToken();
    }

    private AuthResponse buildResponse(User user, String access, String refresh) {
        return AuthResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .accessToken(access)
                .refreshToken(refresh)
                .build();
    }
}

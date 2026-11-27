package com.fooddelivery.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    @Value("${app.jwt.secret}") private String secret;
    @Value("${app.jwt.access-token-expiration}") private long accessExpiry;

    public String generateToken(UserDetails userDetails, Long userId, String role) {
        return Jwts.builder()
                .setClaims(Map.of("userId", userId, "role", role))
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiry))
                .signWith(key(), SignatureAlgorithm.HS256).compact();
    }

    public String extractEmail(String token) { return extractClaim(token, Claims::getSubject); }
    public Long extractUserId(String token)  { return extractClaim(token, c -> c.get("userId", Long.class)); }
    public String extractRole(String token)  { return extractClaim(token, c -> c.get("role", String.class)); }

    public boolean isValid(String token, UserDetails ud) {
        return extractEmail(token).equals(ud.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String t) { return extractClaim(t, Claims::getExpiration).before(new Date()); }

    private <T> T extractClaim(String token, Function<Claims,T> fn) {
        return fn.apply(Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody());
    }

    private Key key() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); }
}

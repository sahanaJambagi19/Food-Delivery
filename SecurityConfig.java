package com.fooddelivery.config;

import com.fooddelivery.security.JwtAuthFilter;
import com.fooddelivery.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.List;

@Configuration @EnableWebSecurity @EnableMethodSecurity @RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService uds;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c->c.disable())
            .cors(c->c.configurationSource(corsSource()))
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers("/", "/index.html", "/login.html", "/register.html", "/cart.html", "/orders.html", "/order-success.html", "/payment.html", "/profile.html", "/restaurant.html", "/review.html").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/dashboard.html", "/restaurant/dashboard.html", "/delivery/dashboard.html").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/coupons/active").permitAll()
                .requestMatchers("/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET,"/restaurants/**","/food-items/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/restaurant-owner/**").hasRole("RESTAURANT_OWNER")
                .requestMatchers("/delivery/**").hasRole("DELIVERY_PARTNER")
                .anyRequest().authenticated())
            .authenticationProvider(authProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of("*"));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Authorization"));
        c.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}

package com.example.uniclub.config;

import com.example.uniclub.security.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final OAuth2FailureHandler oauth2FailureHandler;
    private final CustomAuthEntryPoint customAuthEntryPoint;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CORS + CSRF + session management
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ Exception handling (Unauthorized / Forbidden)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(customAuthEntryPoint)
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\": \"Access Denied - Forbidden\"}");
                        })
                )

                // ✅ Endpoint authorization rules
                .authorizeHttpRequests(auth -> auth
                        // 🌐 Public endpoints (Swagger + Auth)
                        .requestMatchers(
                                "/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // ✅ Public data
                        .requestMatchers(HttpMethod.GET, "/api/university/majors/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/**", "/api/clubs/**").permitAll()

                        // 👤 Profile & Attendance (requires login)
                        .requestMatchers("/api/users/profile/**").authenticated()
                        .requestMatchers("/api/attendance/checkin").authenticated()

                        // 🧩 Wallet APIs
                        // Reward: ClubLeader / UniversityStaff / Admin only
                        .requestMatchers(HttpMethod.POST, "/api/wallets/reward/**")
                        .hasAnyRole("CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")

                        // Get my wallet: any authenticated user
                        .requestMatchers(HttpMethod.GET, "/api/wallets/me").authenticated()

                        // Get club wallet: ClubLeader / Staff / Admin
                        .requestMatchers(HttpMethod.GET, "/api/wallets/club/**")
                        .hasAnyRole("CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")

                        // 🔒 User management – Admin & Staff only
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "UNIVERSITY_STAFF")

                        // 🧩 Specialized API access
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/university/**").hasAnyRole("UNIVERSITY_STAFF", "ADMIN")
                        .requestMatchers("/api/club/**").hasAnyRole("CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")
                        .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")
                        .requestMatchers("/api/attendance/generate/**").hasAnyRole("CLUB_LEADER", "ADMIN")

                        // 🔐 All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // ✅ OAuth2 login configuration
                .oauth2Login(o -> o
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                )

                // ✅ Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 🔐 Authentication Provider (used for traditional login)
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    // 🌍 Global CORS configuration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Location"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

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
                // ✅ Cấu hình CORS + CSRF + session
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ Cấu hình xử lý exception (unauthorized & forbidden)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(customAuthEntryPoint)
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\": \"Access Denied - Forbidden\"}");
                        })
                )

                // ✅ Phân quyền truy cập cho từng nhóm endpoint
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

                        // ✅ Public xem majors, events, clubs
                        .requestMatchers(HttpMethod.GET, "/api/university/majors/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/**", "/api/clubs/**").permitAll()

                        // 👤 Profile và Attendance (cần đăng nhập)
                        .requestMatchers("/api/users/profile/**").authenticated()
                        .requestMatchers("/api/attendance/checkin").authenticated()

                        // 🧩 Wallet APIs
                        // Chỉ CLUB_LEADER, UNIVERSITY_STAFF, ADMIN mới được phát điểm
                        .requestMatchers(HttpMethod.POST, "/api/wallets/reward/**")
                        .hasAnyRole("CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")
                        // Xem ví cá nhân: user nào cũng được sau khi đăng nhập
                        .requestMatchers(HttpMethod.GET, "/api/wallets/me").authenticated()

                        // 🔒 Quản lý người dùng – chỉ ADMIN & STAFF
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "UNIVERSITY_STAFF")

                        // 🧩 Phân quyền đặc thù cho các nhóm API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/university/**").hasAnyRole("UNIVERSITY_STAFF", "ADMIN")
                        .requestMatchers("/api/club/**").hasAnyRole("CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")
                        .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")
                        .requestMatchers("/api/attendance/generate/**").hasAnyRole("CLUB_LEADER", "ADMIN")

                        // 🔐 Các API khác yêu cầu đăng nhập
                        .anyRequest().authenticated()
                )

                // ✅ Cấu hình OAuth2 login
                .oauth2Login(o -> o
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                )

                // ✅ Thêm filter xác thực JWT trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 🔐 Authentication Provider (Dùng cho login truyền thống)
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

    // 🌍 Cấu hình CORS cho toàn hệ thống
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

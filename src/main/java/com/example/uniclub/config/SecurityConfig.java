package com.example.uniclub.config;

import com.example.uniclub.security.CustomAuthEntryPoint;
import com.example.uniclub.security.JwtAuthFilter;
import com.example.uniclub.security.OAuth2FailureHandler;
import com.example.uniclub.security.OAuth2SuccessHandler;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
                // ✅ Cho phép CORS cho FE
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // ✅ Tắt CSRF vì đang dùng JWT
                .csrf(csrf -> csrf.disable())
                // ✅ Stateless (JWT)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // ✅ Xử lý lỗi 401 Unauthorized
                .exceptionHandling(eh -> eh.authenticationEntryPoint(customAuthEntryPoint))

                .authorizeHttpRequests(auth -> auth
                        // 🔓 Public (không cần đăng nhập)
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

                        // 🔓 GET public endpoints
                        .requestMatchers(HttpMethod.GET,
                                "/api/events/**",
                                "/api/clubs/**"
                        ).permitAll()

                        // 🧩 USER MANAGEMENT (ADMIN, UNIVERSITY_ADMIN)
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "UNIVERSITY_ADMIN")

                        // 🧩 EVENT MANAGEMENT
                        .requestMatchers(HttpMethod.POST, "/api/events/**").hasAnyRole("CLUB_MANAGER", "ADMIN", "UNIVERSITY_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/events/**").hasAnyRole("CLUB_MANAGER", "ADMIN", "UNIVERSITY_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasAnyRole("ADMIN", "UNIVERSITY_ADMIN")

                        // 🧩 CLUB MANAGEMENT
                        .requestMatchers(HttpMethod.POST, "/api/clubs/**").hasAnyRole("ADMIN", "CLUB_MANAGER", "UNIVERSITY_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/clubs/**").hasAnyRole("ADMIN", "CLUB_MANAGER", "UNIVERSITY_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/clubs/**").hasAnyRole("ADMIN", "UNIVERSITY_ADMIN")

                        // 🧩 LOCATION MANAGEMENT
                        .requestMatchers("/api/locations/**").hasAnyRole("ADMIN", "UNIVERSITY_ADMIN")

                        // 🧩 UNIVERSITY MANAGEMENT
                        .requestMatchers("/api/university/**").hasRole("UNIVERSITY_ADMIN")

                        // 🧩 ADMIN DASHBOARD
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "UNIVERSITY_ADMIN")

                        // 🧩 STUDENT ZONE
                        .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "CLUB_MANAGER", "ADMIN", "UNIVERSITY_ADMIN")

                        // ✅ Mọi route khác cần đăng nhập
                        .anyRequest().authenticated()
                )

                // 🔐 OAuth2 login (Google)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                )

                // 🔒 JWT Filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ✅ Xác thực qua UserDetailsService
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

    // ✅ Cấu hình CORS cho FE React/Next.js
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

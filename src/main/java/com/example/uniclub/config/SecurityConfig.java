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
import org.springframework.security.config.Customizer;
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
                // ✅ CORS + CSRF + Stateless Session
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
//                .cors(Customizer.withDefaults())
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
                        // ✅ allow CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

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

                                .requestMatchers(HttpMethod.GET, "/api/university/majors/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/events/**", "/api/clubs/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/events/public/attendance/check")
                                .hasRole("STUDENT")
                                .requestMatchers(HttpMethod.POST, "/api/events/checkin")
                                .hasRole("STUDENT")

                                .requestMatchers(HttpMethod.POST, "/api/events/public/checkin")
                                .hasRole("STUDENT")

                                .requestMatchers(HttpMethod.POST, "/api/events/register")
                                .hasRole("STUDENT")

                                .requestMatchers(HttpMethod.POST, "/api/events/*/feedback")
                                .hasRole("STUDENT")

                                .requestMatchers(HttpMethod.POST, "/api/events/**")
                                .hasAnyRole("CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")


                                // 👤 Profile & Attendance
                        .requestMatchers("/api/users/profile/**").authenticated()
                        .requestMatchers("/api/attendance/checkin").authenticated()

                        // 💰 Wallet APIs
                        .requestMatchers(HttpMethod.POST, "/api/wallets/reward/**")
                        .hasAnyRole("CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/wallets/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/wallets/club/**")
                        .hasAnyRole("CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")

                        // 👥 User management
                        .requestMatchers("/api/users/me/clubs").authenticated()
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "UNIVERSITY_STAFF")

                                .requestMatchers(HttpMethod.GET,
                                        "/api/admin/products",
                                        "/api/admin/products/**",
                                        "/api/admin/products/orders/**"
                                ).hasAnyRole("ADMIN", "UNIVERSITY_STAFF")

                        // 🧩 Specialized API access
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/university/**").hasAnyRole("UNIVERSITY_STAFF", "ADMIN")
                        .requestMatchers("/api/club/**").hasAnyRole("CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")
                        .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "CLUB_LEADER", "UNIVERSITY_STAFF", "ADMIN")
                        .requestMatchers("/api/attendance/generate/**").hasAnyRole("CLUB_LEADER", "ADMIN")


                        .requestMatchers("/api/club-activity/**")
                        .hasAnyRole("ADMIN", "UNIVERSITY_STAFF")


                        // 🔒 All other endpoints
                        .anyRequest().authenticated()
                )

                // ✅ OAuth2 login configuration
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                )

                // ✅ JWT filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 🔐 Authentication Provider (for username-password login)
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    // 🔑 BCrypt encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ⚙️ AuthenticationManager (used by AuthController)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    // 🌍 Global CORS configuration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // ✅ Cho phép FE local và deployed domain thật
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:4200",
                "https://uniclub-fpt.vercel.app",
                "https://uniclub.id.vn"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Location"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}

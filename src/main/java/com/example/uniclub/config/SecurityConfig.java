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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(customAuthEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // 🔓 Public APIs
                        .requestMatchers(
                                "/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 🔓 Public GET endpoints
                        .requestMatchers(HttpMethod.GET,
                                "/api/events/**",
                                "/api/products/**",
                                "/api/clubs/**"
                        ).permitAll()

                        // 🔒 Protected endpoints by role
                        .requestMatchers("/api/system/**").hasRole("SYSTEM_MANAGER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/club/**").hasAnyRole("CLUB_MANAGER", "ADMIN", "SYSTEM_MANAGER")
                        .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "CLUB_MANAGER", "ADMIN")
                        .requestMatchers("/api/partner/**").hasAnyRole("PARTNER", "ADMIN")

                        // 🔒 All others require login
                        .anyRequest().authenticated()
                )

                // OAuth2 Login for Google
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                )

                // Add JWT Filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

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

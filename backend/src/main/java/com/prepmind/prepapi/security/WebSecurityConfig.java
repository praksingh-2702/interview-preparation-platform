package com.prepmind.prepapi.security;

import com.prepmind.prepapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtService jwtService,
                                           UserRepository userRepository) throws Exception {
        ProblemJsonSecurityHandlers handlers = new ProblemJsonSecurityHandlers();
        // Created here (not as a @Bean) so Spring Boot does not also register it as a plain servlet filter.
        AuthTokenFilter tokenFilter = new AuthTokenFilter(jwtService, userRepository);

        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)          // stateless bearer tokens, no cookies
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/verify-email",
                        "/api/auth/resend-verification", "/api/auth/login", "/api/auth/logout",
                        "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/google").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/username-available").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/questions").hasRole("EDITOR")
                .requestMatchers(HttpMethod.PUT, "/api/questions/**").hasRole("EDITOR")
                .requestMatchers(HttpMethod.DELETE, "/api/questions/**").hasRole("EDITOR")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String[] origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.copyOf(toSet(origins)));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setAllowCredentials(false); // auth is a bearer header, not cookies
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Backs Spring's AuthenticationManager and stops Boot from creating a default in-memory user. */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(u -> org.springframework.security.core.userdetails.User
                        .withUsername(u.getUsername())
                        .password(u.getPassword())
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static Set<String> toSet(String[] values) {
        if (values == null) {
            return Set.of();
        }
        return Arrays.stream(values)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}

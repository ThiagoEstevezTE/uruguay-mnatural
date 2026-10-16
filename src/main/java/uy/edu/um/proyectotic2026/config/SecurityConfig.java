package uy.edu.um.proyectotic2026.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Mismo algoritmo con el que se guardan las contraseñas al registrarse
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Que puede ver cada rol. Lo que no esta en la lista es publico (portal, registro, API).
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/provider/**").hasRole("PROVIDER")
                        .requestMatchers("/tourist/**").hasRole("TOURIST")
                        .requestMatchers("/panel").authenticated()
                        .anyRequest().permitAll()
                )
                // Pantalla de login propia. Al entrar, /panel manda a cada uno a su espacio.
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/panel")
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/?logout")
                        .permitAll()
                )
                // La API REST la usan otros sistemas, no formularios: no lleva token CSRF
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }
}

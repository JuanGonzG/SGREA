package dgtic.core.config;

import dgtic.core.security.SgreaUserDetailsService;
import dgtic.core.security.JWTAuthenticationFilter;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityAuthenticationConfig {
    // Bean para el filtro de autenticación JWT
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JWTAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        // Autorización de solicitudes HTTP
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        "/login",
                        "/login-bodega",
                        "/login-enter",
                        "/refresh",
                        "/error",
                        "/error/**",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/favicon.ico",
                        "/uploads/productos/**"
                ).permitAll()
                .requestMatchers(
                        "/bodega/**",
                        "/usuarios/**",
                        "/productos/**",
                        "/conjunto/**",
                        "/contenedor/**"
                ).hasRole("ADMINISTRADOR")
                .requestMatchers(
                        "/hojas-produccion/**",
                        "/monitoreo/**",
                        "/reportes/**"
                ).hasAnyRole("ADMINISTRADOR", "SUPERVISOR")
                .requestMatchers(
                        "/hh/surtido/**",
                        "/hh/recepcion/**"
                ).hasAnyRole("ADMINISTRADOR", "OPERADOR")
                .anyRequest().authenticated()
        );

        // Agregar el filtro de autenticación JWT antes del filtro de nombre de usuario y contraseña
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // Configuración de manejo de excepciones
        http.exceptionHandling(exception -> exception
                .accessDeniedPage("/error/403")
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

        // El cierre de sesión de SGREA debe pasar por HomeController para cerrar
        // SesionEntity y expirar también las cookies JWT/Refresh Token.
        http.logout(logout -> logout.disable());

        return http.build();
    }
    // Bean para el proveedor de autenticación DAO
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            SgreaUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
    // Bean para el administrador de autenticación
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(List.of(provider));
    }
}

package com.movie.user_service.security;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.movie.user_service.oauth2.OAuth2LoginFailureHandler;
import com.movie.user_service.oauth2.OAuth2LoginSuccessHandler;
import com.movie.user_service.service.CustomOidcUserDetailsService;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;


@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOidcUserDetailsService customOidcUserDetailsService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final GatewaySecurityExceptionHandler gatewaySecurityExceptionHandler;
    private final OAuth2AuthorizationRequestCookieRepository authorizationRequestRepository;

    @Value("${frontend.app.allowed-origins:${frontend.app.base-url}}")
    private List<String> frontendOrigins;

    @Bean
    @Order (1)
    SecurityFilterChain filterChain1(
            HttpSecurity http) throws Exception{
        // HeaderWriterLogoutHandler clearSiteData = new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(Directive.COOKIES));
        // CookieClearingLogoutHandler cookies = new CookieClearingLogoutHandler("auth-token");
        return http
                // REST calls use bearer tokens. Refresh is protected by a custom request
                // header plus credentialed CORS; OAuth2 uses its signed state cookie.
                .csrf(csrf -> csrf.disable())
                // .cors(cors->cors.configurationSource(corsConfigurationSource()))
                .securityMatcher("/login**","/signup**", "/refresh",
                            "/email-verifications/verify", "/actuator/health/**", "/actuator/info",
                            "/actuator/prometheus")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth-> auth.anyRequest().permitAll())
                .build();
        }

    @Bean
    @Order (2)
    SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            OAuthSignUpFilter signupFlowFilter) throws Exception{
        // HeaderWriterLogoutHandler clearSiteData = new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(Directive.COOKIES));
        // CookieClearingLogoutHandler cookies = new CookieClearingLogoutHandler("auth-token");
        return http
                // REST calls use bearer tokens. Refresh is protected by a custom request
                // header plus credentialed CORS; OAuth2 uses its signed state cookie.
                .csrf(csrf -> csrf.disable())
                // .cors(cors->cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                    .requestMatchers("/login**","/signup**", "/refresh", "/oauth2/**", "/login/oauth2/**",
                            "/email-verifications/verify", "/actuator/health/**", "/actuator/info",
                            "/actuator/prometheus").permitAll()
                    .requestMatchers("/super/**").hasRole("SUPERADMIN")
                    .anyRequest().authenticated()
                    //     .anyRequest().permitAll()
                )
                // .authenticationManager(authenticationManager()) //Manual Configuration can be used with authentication manager
                // Dont use need to figure out how to stop additional calls after success logout it reauthenticating after logout
                // .logout(logout -> logout
                //                         .logoutUrl("/logout")
                //                         .deleteCookies("auth-token")
                //                         // .logoutSuccessUrl("/signup")
                //                         .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                //                         // .addLogoutHandler(clearSiteData)
                //                     .permitAll())
                .addFilterBefore(
                signupFlowFilter,
                OAuth2AuthorizationRequestRedirectFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(gatewaySecurityExceptionHandler)
                        .accessDeniedHandler(gatewaySecurityExceptionHandler)
                )
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestRepository(authorizationRequestRepository))
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserDetailsService))
                        .successHandler(
                                oAuth2LoginSuccessHandler
                        )
                        .failureHandler(
                                oAuth2LoginFailureHandler
                        )
                )
                .build();
    } 

//     @Bean //Authentication Types Manual
// 	public AuthenticationManager authenticationManager() {
// 		DaoAuthenticationProvider daoAuthenticationProviderauthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
// 		daoAuthenticationProviderauthenticationProvider.setPasswordEncoder(passwordEncoder());
// 		return new ProviderManager(daoAuthenticationProviderauthenticationProvider);
// 	}
        @Bean
        AuthenticationManager authenticationManager(
                AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }


        @Bean
        public AuditorAware<String> auditorProvider() {
                return () -> {
                        Authentication auth =
                                SecurityContextHolder.getContext().getAuthentication();

                        if (auth == null ||
                        !auth.isAuthenticated()) {

                        return Optional.of("SYSTEM");
                        }

                        return Optional.of(auth.getName());
                };
        }


        @Bean
        static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("SUPERADMIN").implies("ADMIN")
                .role("ADMIN").implies("USER")
                .build();
        }
        

        @Bean
        public PasswordEncoder passwordEncoder() {
                return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration config=new CorsConfiguration();
        config.setAllowedOrigins(frontendOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Accept", "Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);
        return source;
        }

}

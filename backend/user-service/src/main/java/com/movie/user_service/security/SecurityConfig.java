package com.movie.user_service.security;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpStatus;
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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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


    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,JwtAuthFilter jwtAuthFilter,OAuthSignUpFilter signupFlowFilter) throws Exception{
        // HeaderWriterLogoutHandler clearSiteData = new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(Directive.COOKIES));
        // CookieClearingLogoutHandler cookies = new CookieClearingLogoutHandler("auth-token");
        return http
                .csrf(csrf->csrf.disable())
                .cors(cors->cors.configurationSource(corsConfigurationSource()))
                .anonymous(anonymous -> anonymous.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                    .requestMatchers("/login**","/signup**","/signout**","/oauth2/**","/delete**").permitAll()
                    .requestMatchers("/superadmin").hasRole("SUPERADMIN")
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
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                )
                .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
                )
                .oauth2Login(oauth -> oauth.defaultSuccessUrl("/dashboard")
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
        config.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        config.setAllowedMethods(Arrays.asList("*"));
        config.setAllowedHeaders(Arrays.asList("*"));  
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);
        return source;
        }
}

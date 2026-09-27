package com.movie.user_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.movie.user_service.entity.AuthProvider;
import com.movie.user_service.entity.OAuthAccount;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.User;
import com.movie.user_service.exceptions.AlreadyExistsException;
import com.movie.user_service.repository.OAuthRepository;
import com.movie.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OAuth2UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuthRepository oAuthRepository;

    @InjectMocks
    private OAuth2UserRegistrationService registrationService;

    @Test
    void linksFirstGoogleLoginToExistingLocalUser() {
        User localUser = user(1L, "local@example.com", "encoded-password");
        Role userRole = new Role();

        when(oAuthRepository.findByProviderSubjectAndAuthProvider("google-subject", AuthProvider.GOOGLE))
                .thenReturn(List.of());
        when(userRepository.findByEmail("local@example.com")).thenReturn(Optional.of(localUser));
        when(userRepository.save(localUser)).thenReturn(localUser);

        User result = registrationService.processUserRegistration(
                "local@example.com", "Google User", "google-subject", AuthProvider.GOOGLE, Set.of(userRole));

        assertSame(localUser, result);
        assertEquals("encoded-password", localUser.getPassword());
        assertEquals(1, localUser.getOauthAccounts().size());
        assertEquals("google-subject", localUser.getOauthAccounts().getFirst().getProviderSubject());
        verify(userRepository).save(localUser);
    }

    @Test
    void updatesKnownOAuthAccountEmailOnlyWhenItIsAvailable() {
        User oauthUser = user(2L, "old@example.com", null);
        OAuthAccount account = new OAuthAccount();
        account.setUser(oauthUser);

        when(oAuthRepository.findByProviderSubjectAndAuthProvider("google-subject", AuthProvider.GOOGLE))
                .thenReturn(List.of(account));
        when(userRepository.existsByEmailAndIdNot("new@example.com", 2L)).thenReturn(false);
        when(userRepository.findByIdWithRoleAndPrivilege(2L)).thenReturn(Optional.of(oauthUser));

        User result = registrationService.processUserRegistration(
                "new@example.com", "Google User", "google-subject", AuthProvider.GOOGLE, Set.of());

        assertSame(oauthUser, result);
        assertEquals("new@example.com", oauthUser.getEmail());
        verify(userRepository).existsByEmailAndIdNot("new@example.com", 2L);
        verify(userRepository, never()).save(oauthUser);
    }

    @Test
    void rejectsOAuthEmailChangeThatWouldCollideWithAnotherUser() {
        User oauthUser = user(2L, "old@example.com", null);
        OAuthAccount account = new OAuthAccount();
        account.setUser(oauthUser);

        when(oAuthRepository.findByProviderSubjectAndAuthProvider("google-subject", AuthProvider.GOOGLE))
                .thenReturn(List.of(account));
        when(userRepository.findByIdWithRoleAndPrivilege(2L)).thenReturn(Optional.of(oauthUser));
        when(userRepository.existsByEmailAndIdNot("taken@example.com", 2L)).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> registrationService.processUserRegistration(
                "taken@example.com", "Google User", "google-subject", AuthProvider.GOOGLE, Set.of()));
        assertEquals("old@example.com", oauthUser.getEmail());
    }

    private User user(Long id, String email, String password) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword(password);
        user.setEmailVerified(true);
        return user;
    }
}

package com.finaudit.api.security;

import com.finaudit.api.entity.User;
import com.finaudit.api.repository.UserRepository;
import com.finaudit.core.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("loadUserByUsername should return UserPrincipal when user exists")
    void shouldLoadUserByUsername() {
        User user = new User("admin@finaudit.ai", "pass", UserRole.ADMIN);
        user.setId(5L);

        when(userRepository.findByEmail("admin@finaudit.ai")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin@finaudit.ai");

        assertThat(details.getUsername()).isEqualTo("admin@finaudit.ai");
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername should throw UsernameNotFoundException when user does not exist")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("missing@finaudit.ai")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@finaudit.ai"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing@finaudit.ai");
    }
}

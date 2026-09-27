package com.finaudit.api.repository;

import com.finaudit.api.entity.User;
import com.finaudit.core.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save user and find by email")
    void shouldSaveAndFindByEmail() {
        User user = new User("auditor@example.com", "hashed_pwd_123", UserRole.AUDITOR);
        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();

        Optional<User> found = userRepository.findByEmail("auditor@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(UserRole.AUDITOR);
        assertThat(userRepository.existsByEmail("auditor@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }
}

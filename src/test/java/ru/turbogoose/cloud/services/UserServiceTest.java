package ru.turbogoose.cloud.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.turbogoose.cloud.exceptions.UsernameAlreadyExistsException;
import ru.turbogoose.cloud.models.User;
import ru.turbogoose.cloud.repositories.UserRepository;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class UserServiceTest {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserService userService;

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"));

    @Autowired
    UserServiceTest(PasswordEncoder passwordEncoder, UserRepository userRepository, UserService userService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @DynamicPropertySource
    public static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @AfterEach
    public void clearTable() {
        userRepository.deleteAll();
    }

    @Test
    public void whenCreateUserThenCreate() {
        userService.createUser(new User("User", "Password"));

        List<User> users = userRepository.findAll();
        assertThat(users.size(), is(1));

        User retrievedUser = users.get(0);
        assertThat(retrievedUser.getUsername(), is("User"));
        assertThat(passwordEncoder.matches("Password", retrievedUser.getPassword()), is(true));
    }

    @Test
    public void whenCreateUserWithDuplicatedUsernameThenThrow() {
        User user = new User("User", "Password");
        userService.createUser(user);

        assertThrows(UsernameAlreadyExistsException.class, () -> userService.createUser(user));
        assertThat(userRepository.count(), is(1L));
    }

    @Test
    public void whenLoadUserByUsernameThenReturn() {
        userService.createUser(new User("User", "Password"));

        UserDetails userDetails = userService.loadUserByUsername("User");

        assertThat(userDetails.getUsername(), is("User"));
        assertThat(passwordEncoder.matches("Password", userDetails.getPassword()), is(true));
        assertThat(userDetails.isEnabled(), is(true));
        assertThat(userDetails.isAccountNonExpired(), is(true));
        assertThat(userDetails.isCredentialsNonExpired(), is(true));
        assertThat(userDetails.isAccountNonLocked(), is(true));
    }

    @Test
    public void whenLoadUserByNonExistentUsernameThenThrow() {
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("Bebrik"));
    }
}
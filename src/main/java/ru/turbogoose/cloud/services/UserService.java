package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.turbogoose.cloud.exceptions.UsernameAlreadyExistsException;
import ru.turbogoose.cloud.models.User;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.repositories.FileRepository;
import ru.turbogoose.cloud.repositories.ObjectPathFactory;
import ru.turbogoose.cloud.repositories.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileRepository fileRepository;
    private final ObjectPathFactory objectPathFactory;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            LOGGER.debug("User \"{}\" not found", username);
            throw new UsernameNotFoundException(String.format("User with username \"%s\" not found", username));
        }
        User user = optionalUser.get();
        LOGGER.debug("User \"{}\" (id={}) loaded successfully", username, user.getId());
        return new UserDetailsImpl(user);
    }

    @Transactional
    public void createUser(User user) {
        if (user == null) {
            LOGGER.warn("Failed to create user: null provided");
            return;
        }
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
            LOGGER.debug("User with name \"{}\" saved. Assigned id: {}", user.getUsername(), user.getId());
            createUserHomeFolder(user.getId());
        } catch (DataIntegrityViolationException exc) {
            throw new UsernameAlreadyExistsException(
                    String.format("User with username \"%s\" already exists", user.getUsername()), exc);
        }
    }

    private void createUserHomeFolder(int userId) {
        fileRepository.createFolder(objectPathFactory.getRootFolder(userId));
        LOGGER.debug("Root folder for user with id={} created", userId);
    }
}

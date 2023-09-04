package ru.turbogoose.cloud.services;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.turbogoose.cloud.exceptions.UsernameAlreadyExistsException;
import ru.turbogoose.cloud.repositories.minio.MinioObjectPath;
import ru.turbogoose.cloud.models.User;
import ru.turbogoose.cloud.models.security.UserDetailsImpl;
import ru.turbogoose.cloud.repositories.minio.MinioRepository;
import ru.turbogoose.cloud.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioRepository minioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(UserDetailsImpl::new)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("User with username %s not found", username)
        ));
    }

    @Transactional
    public void createUser(User user) {
        if (user == null) {
            return;
        }
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
            createUserHomeFolder(user.getId());
        } catch (DataIntegrityViolationException exc) {
            throw new UsernameAlreadyExistsException(
                    String.format("User with username %s already exists", user.getUsername()), exc);
        }
    }

    private void createUserHomeFolder(int userId) {
        minioRepository.createFolder(MinioObjectPath.getRootFolder(userId));
    }
}

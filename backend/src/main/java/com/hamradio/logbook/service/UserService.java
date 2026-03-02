package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set default roles
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.addRole(User.Role.ROLE_USER);
        }

        return userRepository.save(user);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(Long id, User userUpdates) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (userUpdates.getFullName() != null) {
            user.setFullName(userUpdates.getFullName());
        }
        if (userUpdates.getCallsign() != null) {
            user.setCallsign(userUpdates.getCallsign());
        }
        if (userUpdates.getGridSquare() != null) {
            user.setGridSquare(userUpdates.getGridSquare());
        }
        if (userUpdates.getQrzApiKey() != null) {
            user.setQrzApiKey(userUpdates.getQrzApiKey());
        }

        return userRepository.save(user);
    }

    public void updateLastLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public Optional<String> getStationColorPreferences(String username) {
        return userRepository.findByUsername(username)
                .map(User::getStationColorPreferences);
    }

    public void saveStationColorPreferences(String username, String colorsJson) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStationColorPreferences(colorsJson);
        userRepository.save(user);
    }

    public void resetStationColorPreferences(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStationColorPreferences(null);
        userRepository.save(user);
    }
}

package com.aibot.llamachatbot.service;

import com.aibot.llamachatbot.dto.user.UserResponse;
import com.aibot.llamachatbot.entity.User;
import com.aibot.llamachatbot.exception.UserNotFoundException;
import com.aibot.llamachatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
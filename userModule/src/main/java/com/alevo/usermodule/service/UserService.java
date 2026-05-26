package com.alevo.usermodule.service;


import com.alevo.usermodule.dto.request.UpdateProfileRequest;
import com.alevo.usermodule.dto.response.UserResponse;
import com.alevo.usermodule.entity.UserAccount;
import com.alevo.usermodule.enums.UserStatus;
import com.alevo.usermodule.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserResponse getProfile(UserAccount user) {
        return authService.mapUser(user);
    }

    @Transactional
    public UserResponse updateProfile(UserAccount user, UpdateProfileRequest req) {
        if (req.getDisplayName() != null) user.setDisplayName(req.getDisplayName());
        if (req.getAbout() != null) user.setAbout(req.getAbout());
        if (req.getProfilePictureUrl() != null) user.setProfilePictureUrl(req.getProfilePictureUrl());
        UserAccount saved = userRepository.save(user);
        log.info("Profile updated for user {}", user.getPhoneNumber());
        return authService.mapUser(saved);
    }

    @Transactional
    public void updateStatus(UserAccount user, UserStatus status) {
        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional
    public void deactivateAccount(UserAccount user) {
        user.setActive(false);
        userRepository.save(user);
        log.warn("Account deactivated: {}", user.getPhoneNumber());
    }
}

package com.alevo.usermodule.controller;

import com.alevo.usermodule.dto.request.UpdateProfileRequest;
import com.alevo.usermodule.dto.response.ApiResponse;
import com.alevo.usermodule.dto.response.UserResponse;
import com.alevo.usermodule.enums.UserStatus;
import com.alevo.usermodule.security.AuthenticatedPrincipal;
import com.alevo.usermodule.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * User profile management.
 *
 * GET   /api/users/me            → Fetch own profile
 * PUT   /api/users/me            → Update name, about, avatar
 * PATCH /api/users/me/status     → Update online status
 * DELETE /api/users/me           → Deactivate account
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched",
                userService.getProfile(principal.getUser())));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest req,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        UserResponse updated = userService.updateProfile(principal.getUser(), req);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", updated));
    }

    @PatchMapping("/me/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @RequestParam String status,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
        userService.updateStatus(principal.getUser(), userStatus);
        return ResponseEntity.ok(ApiResponse.ok("Status updated to " + status));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        userService.deactivateAccount(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok("Account deactivated"));
    }
}

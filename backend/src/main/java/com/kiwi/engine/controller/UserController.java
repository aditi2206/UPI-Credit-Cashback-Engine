package com.kiwi.engine.controller;

import com.kiwi.engine.dto.Dtos.*;
import com.kiwi.engine.entity.User;
import com.kiwi.engine.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserSummaryResponse> getUserSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserSummary(userId));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}

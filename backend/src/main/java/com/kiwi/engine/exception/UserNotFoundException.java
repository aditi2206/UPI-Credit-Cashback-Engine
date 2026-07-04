package com.kiwi.engine.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User not found: " + userId);
    }
}

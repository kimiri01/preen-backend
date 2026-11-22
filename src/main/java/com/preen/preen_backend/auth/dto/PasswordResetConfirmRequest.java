package com.preen.preen_backend.auth.dto;

public record PasswordResetConfirmRequest(String token, String newPassword) {
}

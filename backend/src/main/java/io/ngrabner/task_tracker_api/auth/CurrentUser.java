package io.ngrabner.task_tracker_api.auth;

public record CurrentUser(Long userId, String email) {
}

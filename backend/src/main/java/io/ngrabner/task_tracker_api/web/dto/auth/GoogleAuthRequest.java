package io.ngrabner.task_tracker_api.web.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class GoogleAuthRequest {

    @NotBlank
    private String idToken;

    public GoogleAuthRequest() {
    }

    public GoogleAuthRequest(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}

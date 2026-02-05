package io.ngrabner.task_tracker_api.auth;

import com.google.auth.oauth2.TokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifierService {

    private final String googleClientId;

    public GoogleTokenVerifierService(@Value("${app.google.client-id:}") String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public GoogleUserInfo verify(String idToken) throws Exception {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID is not set on the backend.");
        }

        TokenVerifier verifier = TokenVerifier.newBuilder()
                .setAudience(googleClientId)
                .build();

        JsonWebSignature token = verifier.verify(idToken);
        JsonWebToken.Payload payload = token.getPayload();

        String sub = payload.getSubject();
        String email = (String) payload.get("email");
        String name = (String) payload.get("name");

        return new GoogleUserInfo(sub, email, name);
    }

    public record GoogleUserInfo(String sub, String email, String name) {
    }
}

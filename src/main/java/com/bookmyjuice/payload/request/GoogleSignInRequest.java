package com.bookmyjuice.payload.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for Google Sign-In.
 * Contains the Google ID token obtained from the Flutter google_sign_in plugin.
 */
public class GoogleSignInRequest {

    @NotBlank
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}

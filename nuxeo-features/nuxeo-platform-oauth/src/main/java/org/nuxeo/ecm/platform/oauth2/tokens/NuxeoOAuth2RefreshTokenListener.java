package org.nuxeo.ecm.platform.oauth2.tokens;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;

import java.io.IOException;

public class NuxeoOAuth2RefreshTokenListener implements CredentialRefreshListener {

    private final OAuth2TokenStore credentialDataStore;

    public NuxeoOAuth2RefreshTokenListener(OAuth2TokenStore credentialDataStore) {
        this.credentialDataStore = credentialDataStore;
    }

    @Override
    public void onTokenResponse(Credential credential, TokenResponse tokenResponse) throws IOException {
        StoredCredential storedCredential = new StoredCredential();
        storedCredential.setAccessToken(credential.getAccessToken());
        storedCredential.setExpirationTimeMilliseconds(credential.getExpirationTimeMilliseconds());
        storedCredential.setRefreshToken(credential.getRefreshToken());
        credentialDataStore.refresh(storedCredential);
    }

    @Override
    public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) throws IOException {
    }
}

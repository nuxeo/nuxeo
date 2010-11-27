package org.nuxeo.ecm.automation.client.jaxrs.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.impl.auth.AuthSchemeBase;
import org.apache.http.util.CharArrayBuffer;

public class SharedSecretScheme extends AuthSchemeBase {

    @Override
    public String getSchemeName() {
        return "nx-shared-secret";
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public String getRealm() {
        return null;
    }

    @Override
    public boolean isConnectionBased() {
        return false;
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public Header authenticate(Credentials credentials, HttpRequest request) throws AuthenticationException {
        String secretKey = credentials.getPassword();
        String userName = credentials.getUserPrincipal().getName();
        // compute token

        Date timestamp = new Date();
        int randomData = new Random(timestamp.getTime()).nextInt();

        String clearToken = String.format("%d:%s:%s", timestamp.getTime(), secretKey, userName);

        byte[] hashedToken;

        try {
            hashedToken = MessageDigest.getInstance("MD5").digest(clearToken.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Cannot compute token", e);
        }

        String base64HashedToken = Base64.encodeBase64String(hashedToken);

        // set request headers

        request.addHeader("NX_TS", String.valueOf(timestamp.getTime()));
        request.addHeader("NX_RD", String.valueOf(randomData));
        request.addHeader("NX_TOKEN", base64HashedToken);
        request.addHeader("NX_USER", userName);

        return null;
    }

    @Override
    protected void parseChallenge(CharArrayBuffer buffer, int pos, int len) throws MalformedChallengeException {

    }

}

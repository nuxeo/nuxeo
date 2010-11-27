package org.nuxeo.ecm.automation.client.jaxrs.spi.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;

import org.nuxeo.ecm.automation.client.jaxrs.RequestInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;
import org.nuxeo.ecm.automation.client.jaxrs.util.Base64;

public class PortalSSOAuthInterceptor implements RequestInterceptor {

    protected final String secret;
    protected final String username;


    public PortalSSOAuthInterceptor(String secretKey, String userName) {
        this.secret = secretKey;
        this.username = userName;
    }

    @Override
    public void processRequest(Request request, Connector connector) {
                // compute token

        long ts = new Date().getTime();
        long random = new Random(ts).nextInt();

        String clearToken = String.format("%d:%d:%s:%s", ts, random, secret, username);

        byte[] hashedToken;

        try {
            hashedToken = MessageDigest.getInstance("MD5").digest(clearToken.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Cannot compute token", e);
        }

        String base64HashedToken = Base64.encode(hashedToken);

        // set request headers

        request.put("NX_TS", String.valueOf(ts));
        request.put("NX_RD", String.valueOf(random));
        request.put("NX_TOKEN", base64HashedToken);
        request.put("NX_USER", username);


    }

}

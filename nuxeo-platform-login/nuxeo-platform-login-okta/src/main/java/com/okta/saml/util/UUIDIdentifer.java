package com.okta.saml.util;

import java.util.UUID;

/**
 * Implementation for Identifier that generates a UUID string on the fly
 */
public class UUIDIdentifer implements Identifier {

    public String getId() {
        return UUID.randomUUID().toString();
    }
}

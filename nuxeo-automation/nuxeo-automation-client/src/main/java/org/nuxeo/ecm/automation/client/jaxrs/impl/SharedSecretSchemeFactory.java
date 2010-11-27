package org.nuxeo.ecm.automation.client.jaxrs.impl;

import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.params.HttpParams;

public class SharedSecretSchemeFactory implements AuthSchemeFactory {

    @Override
    public AuthScheme newInstance(HttpParams params) {
        return new SharedSecretScheme();
    }

}

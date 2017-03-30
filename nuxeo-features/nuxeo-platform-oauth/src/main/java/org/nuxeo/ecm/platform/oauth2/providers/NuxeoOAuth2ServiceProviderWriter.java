/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.ecm.platform.oauth2.providers;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

/**
 * @since 9.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoOAuth2ServiceProviderWriter extends ExtensibleEntityJsonWriter<NuxeoOAuth2ServiceProvider> {

    public static final String ENTITY_TYPE = "nuxeoOAuth2ServiceProvider";

    public NuxeoOAuth2ServiceProviderWriter() {
        super(ENTITY_TYPE, NuxeoOAuth2ServiceProvider.class);
    }

    @Override
    protected void writeEntityBody(NuxeoOAuth2ServiceProvider provider, JsonGenerator jg) throws IOException {
        UserManager userManager = Framework.getService(UserManager.class);
        String principalName = ctx.getSession(null).getSession().getPrincipal().getName();
        NuxeoPrincipal principal = userManager.getPrincipal(principalName);
        jg.writeStringField("serviceName", provider.getServiceName());
        jg.writeStringField("description", provider.getDescription());
        jg.writeStringField("clientId", provider.getClientId());
        jg.writeStringField("clientSecret", principal.isAdministrator() ? provider.getClientSecret() : null);
        jg.writeStringField("authorizationServerURL", provider.getAuthorizationServerURL());
        jg.writeStringField("tokenServerURL", provider.getTokenServerURL());
        jg.writeStringField("userAuthorizationURL", provider.getUserAuthorizationURL());
        jg.writeArrayFieldStart("scopes");
        for (String scope : provider.getScopes()) {
            jg.writeString(scope);
        }
        jg.writeEndArray();
        jg.writeBooleanField("isEnabled", provider.isEnabled());

        jg.writeBooleanField("isAvailable", provider.isProviderAvailable());
        String authorizationURL = null;
        if (provider.getClientId() != null) {
            try {
                authorizationURL = provider.getAuthorizationUrl(ctx.getBaseUrl());
            } catch (IllegalArgumentException e) {
                authorizationURL = null;
            }
        }
        jg.writeStringField("authorizationURL", authorizationURL);
        NuxeoOAuth2Token token = getToken(provider, principalName);
        boolean isAuthorized = (token != null);
        jg.writeBooleanField("isAuthorized", isAuthorized);
        jg.writeStringField("userId", isAuthorized ? token.getServiceLogin() : null);
    }

    private NuxeoOAuth2Token getToken(NuxeoOAuth2ServiceProvider provider, String nxuser) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("serviceName", provider.getId());
        filter.put(NuxeoOAuth2Token.KEY_NUXEO_LOGIN, nxuser);
        return Framework.doPrivileged(() -> {
            List<DocumentModel> entries = provider.getCredentialDataStore().query(filter);
            if (entries != null) {
                if (entries.size() > 1) {
                    throw new NuxeoException("Found multiple " + provider.getId() + " accounts for " + nxuser);
                } else if (entries.size() == 1) {
                    return new NuxeoOAuth2Token(entries.get(0));
                }
            }
            return null;
        });
    }
}

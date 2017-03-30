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

import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

/**
 * @since 9.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoOAuth2ServiceProviderReader extends EntityJsonReader<NuxeoOAuth2ServiceProvider> {

    public NuxeoOAuth2ServiceProviderReader() {
        super(NuxeoOAuth2ServiceProviderWriter.ENTITY_TYPE);
    }

    @Override
    protected NuxeoOAuth2ServiceProvider readEntity(JsonNode jn) throws IOException {
        NuxeoOAuth2ServiceProvider provider = new NuxeoOAuth2ServiceProvider();
        provider.setServiceName(getStringField(jn, "serviceName"));
        provider.setDescription(getStringField(jn, "description"));
        provider.setClientId(getStringField(jn, "clientId"));
        provider.setClientSecret(getStringField(jn, "clientSecret"));
        provider.setAuthorizationServerURL(getStringField(jn, "authorizationServerURL"));
        provider.setTokenServerURL(getStringField(jn, "tokenServerURL"));
        provider.setUserAuthorizationURL(getStringField(jn, "userAuthorizationURL"));
        List<String> scopes = getStringListField(jn, "scopes");
        provider.setScopes(scopes == null ? new String[0] : scopes.toArray(new String[0]));
        Boolean enabled = getBooleanField(jn, "isEnabled");
        provider.setEnabled(enabled == null ? false : enabled);
        return provider;
    }


}

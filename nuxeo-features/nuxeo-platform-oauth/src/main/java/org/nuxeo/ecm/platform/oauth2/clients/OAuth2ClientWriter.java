/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.oauth2.clients;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client.NAME_FIELD;
import static org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client.REDIRECT_URI_FIELD;

import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class OAuth2ClientWriter extends ExtensibleEntityJsonWriter<OAuth2Client> {

    public static final String ENTITY_TYPE = "oauth2Client";

    /**
     * @since 11.1
     */
    public static final String ID_FIELD = "id";

    /**
     * @since 11.1
     */
    public static final String SECRET_FIELD = "secret";

    /**
     * @since 11.1
     */
    public static final String ENABLED_FIELD = "isEnabled";

    /**
     * @since 11.1
     */
    public static final String AUTO_GRANT_FIELD = "isAutoGrant";

    public OAuth2ClientWriter() {
        super(ENTITY_TYPE, OAuth2Client.class);
    }

    @Override
    protected void writeEntityBody(OAuth2Client client, JsonGenerator jg) throws IOException {
        jg.writeStringField(NAME_FIELD, client.getName());
        jg.writeArrayFieldStart(REDIRECT_URI_FIELD);
        for (String url : client.getRedirectURIs()) {
            jg.writeString(url);
        }
        jg.writeEndArray();
        jg.writeStringField(SECRET_FIELD, client.getSecret());
        jg.writeStringField(ID_FIELD, client.getId());
        jg.writeBooleanField(AUTO_GRANT_FIELD, client.isAutoGrant());
        jg.writeBooleanField(ENABLED_FIELD, client.isEnabled());
    }
}

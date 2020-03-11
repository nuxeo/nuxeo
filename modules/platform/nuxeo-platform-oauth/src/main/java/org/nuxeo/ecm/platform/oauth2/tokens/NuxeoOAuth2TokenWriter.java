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
package org.nuxeo.ecm.platform.oauth2.tokens;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 9.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoOAuth2TokenWriter extends ExtensibleEntityJsonWriter<NuxeoOAuth2Token> {

    public static final String ENTITY_TYPE = "nuxeoOAuth2Token";

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public NuxeoOAuth2TokenWriter() {
        super(ENTITY_TYPE, NuxeoOAuth2Token.class);
    }

    @Override
    protected void writeEntityBody(NuxeoOAuth2Token token, JsonGenerator jg) throws IOException {
        jg.writeStringField("serviceName", token.getServiceName());
        jg.writeStringField("nuxeoLogin", token.getNuxeoLogin());
        jg.writeStringField("serviceLogin", token.getServiceLogin());
        jg.writeStringField("clientId", token.getClientId());
        jg.writeBooleanField("isShared", token.isShared());
        jg.writeArrayFieldStart("sharedWith");
        String sharedWithStr = token.getSharedWith();
        String[] sharedWith = sharedWithStr == null ? new String[0] : token.getSharedWith().split(",");
        for (String user : sharedWith) {
            jg.writeString(user.trim());
        }
        jg.writeEndArray();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        Calendar cal = token.getCreationDate();
        jg.writeStringField("creationDate", dateFormat.format(cal == null ? null : cal.getTime()));
    }
}

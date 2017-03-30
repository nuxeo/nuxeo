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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

/**
 * @since 9.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoOAuth2TokenReader extends EntityJsonReader<NuxeoOAuth2Token> {

    public NuxeoOAuth2TokenReader() {
        super(NuxeoOAuth2TokenWriter.ENTITY_TYPE);
    }

    @Override
    protected NuxeoOAuth2Token readEntity(JsonNode jn) throws IOException {
        String cliendId = getStringField(jn, "clientId");
        NuxeoOAuth2Token token = new NuxeoOAuth2Token(0, cliendId);
        token.setServiceName(getStringField(jn, "serviceName"));
        token.setNuxeoLogin(getStringField(jn, "nuxeoLogin"));
        token.setServiceLogin(getStringField(jn, "serviceLogin"));
        SimpleDateFormat dateFormat = new SimpleDateFormat(NuxeoOAuth2TokenWriter.DATE_FORMAT);
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(dateFormat.parse(getStringField(jn, "creationDate")));
        } catch (ParseException e) {
            cal = null;
        }
        token.setCreationDate(cal);
        token.setIsShared(getBooleanField(jn, "isShared"));
        List<String> sharedWith = getStringListField(jn, "sharedWith");
        token.setSharedWith(sharedWith == null ? "" : String.join(",", sharedWith));
        return token;
    }

}

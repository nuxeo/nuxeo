/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.tokenauth.io;

import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Convert {@link AuthenticationToken} to Json.
 *
 * @since 8.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class AuthenticationTokenWriter extends AbstractJsonWriter<AuthenticationToken> {

    public static final String ENTITY_TYPE = "token";

    @Override
    public void write(AuthenticationToken token, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField(ENTITY_FIELD_NAME, ENTITY_TYPE);
        jg.writeStringField("id", token.getToken());
        jg.writeStringField("username", token.getUserName());
        jg.writeStringField("application", token.getApplicationName());
        jg.writeStringField("deviceId", token.getDeviceId());
        jg.writeStringField("deviceDescription", token.getDeviceDescription());
        jg.writeStringField("permission", token.getPermission());
        DateTimeFormatter dateTime = ISODateTimeFormat.dateTime();
        jg.writeStringField("creationDate", dateTime.print(new DateTime(token.getCreationDate())));
        jg.writeEndObject();
    }
}

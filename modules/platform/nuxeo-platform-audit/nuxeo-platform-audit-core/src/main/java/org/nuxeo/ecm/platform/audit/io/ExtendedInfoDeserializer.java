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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.audit.io;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.core.util.Base64;

/**
 * Deserializer class for extended info from a JSON object
 *
 * @since 9.3
 */
public class ExtendedInfoDeserializer extends JsonDeserializer<ExtendedInfo> {

    @Override
    public ExtendedInfo deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = mapper.readTree(jp);
        Serializable value;
        switch (node.getNodeType()) {
        case STRING:
            value = node.textValue();
            try {
                value = Date.from(Instant.parse((String) value));
            } catch (DateTimeParseException e) {
                // ignore
            }
            break;
        case BOOLEAN:
            value = node.booleanValue();
            break;
        case NUMBER:
            value = node.numberValue();
            if (value instanceof Integer) {
                // convert it to long, it is the original type and json can't differentiate int and long
                value = Long.valueOf((Integer) value);
            }
            break;
        case BINARY:
            value = SerializationUtils.deserialize(Base64.decode(node.binaryValue()));
            break;
        case ARRAY:
            value = (Serializable) mapper.convertValue(node, List.class);
            break;
        case OBJECT:
            value = (Serializable) mapper.convertValue(node, Map.class);
            break;
        default:
            throw new UnsupportedOperationException("Error when deserializing type: " + node.getNodeType());
        }
        return Framework.getService(AuditLogger.class).newExtendedInfo(value);
    }

}

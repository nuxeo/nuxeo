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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.core.util.Base64;
import org.apache.commons.lang3.SerializationUtils;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Deserializer class for extended info from a JSON object
 * 
 * @since 9.3
 */
public class ExtendedInfoDeserializer extends JsonDeserializer<ExtendedInfo> {

    @Override
    public ExtendedInfo deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

        ExtendedInfo info;
        JsonNode node = jp.getCodec().readTree(jp);
        switch (node.getNodeType()) {
        case STRING:
            String value = node.textValue();
            try {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                dateFormatter.setLenient(false);
                Date date = dateFormatter.parse(value);
                info = new ExtendedInfoImpl.DateInfo(date);
            } catch (ParseException e) {
                info = new ExtendedInfoImpl.StringInfo(value);
            }
            break;
        case BOOLEAN:
            info = new ExtendedInfoImpl.BooleanInfo(node.booleanValue());
            break;
        case NUMBER:
            Number number = node.numberValue();
            if (number instanceof Double) {
                info = new ExtendedInfoImpl.DoubleInfo(node.doubleValue());
            } else {
                info = new ExtendedInfoImpl.LongInfo(node.longValue());
            }
            break;
        case BINARY:
            info = new ExtendedInfoImpl.BlobInfo(deserializeFromByteArray(node.binaryValue()));
            break;
        default:
            throw new UnsupportedOperationException("Error when deserializing type: " + node.getNodeType());
        }
        return info;
    }

    protected static Serializable deserializeFromByteArray(byte[] byteArray) {
        return SerializationUtils.deserialize(Base64.decode(byteArray));
    }

}

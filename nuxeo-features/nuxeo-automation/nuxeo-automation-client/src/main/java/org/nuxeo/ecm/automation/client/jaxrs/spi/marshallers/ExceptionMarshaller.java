/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import java.io.IOException;

import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * @author matic
 */
public class ExceptionMarshaller implements JsonMarshaller<RemoteException> {

    @Override
    public String getType() {
        return "exception";
    }

    @Override
    public Class<RemoteException> getJavaType() {
        return RemoteException.class;
    }

    public static RemoteException readException(String content) throws IOException {
        try (JsonParser jp = JsonMarshalling.getFactory().createParser(content)) {
            jp.nextToken(); // skip {
            return _read(jp);
        }
    }

    @Override
    public RemoteException read(JsonParser jp) throws IOException {
        return _read(jp);
    }

    public static RemoteException _read(JsonParser jp) throws IOException {
        int status = 0;
        String type = null, message = null;
        Throwable cause = null;
        JsonToken tok = jp.nextToken();
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            if ("status".equals(key)) {
                if (tok == JsonToken.VALUE_NUMBER_INT) {
                    status = jp.getIntValue();
                } else {
                    status = Integer.parseInt(jp.getText());
                }
            } else if (Constants.KEY_ENTITY_TYPE.equals(key) || "type".equals(key)) {
                type = jp.getText();
            } else if ("message".equals(key)) {
                message = jp.getText();
            } else if ("exception".equals(key) || "cause".equals(key)) {
                cause = jp.readValueAs(Throwable.class);
            }
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
        return new RemoteException(status, type, message, cause);
    }

    @Override
    public void write(JsonGenerator jg, Object value) {
        throw new UnsupportedOperationException();
    }

}

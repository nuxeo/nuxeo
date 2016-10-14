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
 *         Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.client.test.marshallers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.client.api.marshaller.NuxeoMarshaller;
import org.nuxeo.client.api.objects.Document;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * @since 0.1
 */
public class DocumentMarshaller implements NuxeoMarshaller<Document> {

    @Override
    public Class<Document> getJavaType() {
        return Document.class;
    }

    @Override
    public Document read(JsonParser jp) throws IOException {
        return readDocument(jp);
    }

    protected static Document readDocument(JsonParser jp) throws IOException {
        String uid = null;
        String type = null;
        String path = null;
        String state = null;
        String versionLabel = null;
        String isCheckedOut = null;
        String lockCreated = null;
        String lockOwner = null;
        String repository = null;
        String changeToken = null;
        JsonToken tok = jp.nextToken();
        Map<String, Object> properties = new HashMap<>();
        while (tok != null && tok != JsonToken.END_OBJECT) {
            tok = jp.nextToken();
            String key = jp.getText();
            tok = jp.nextToken();
            if ("uid".equals(key)) {
                uid = jp.getText();
            } else if ("path".equals(key)) {
                path = jp.getText();
            } else if ("type".equals(key)) {
                type = jp.getText();
            } else if ("state".equals(key)) {
                state = jp.getText();
            } else if ("versionLabel".equals(key)) {
                versionLabel = jp.getText();
            } else if ("isCheckedOut".equals(key)) {
                isCheckedOut = jp.getText();
            } else if ("lock".equals(key)) {
                if (!JsonToken.VALUE_NULL.equals(jp.getCurrentToken())) {
                    String[] lock = jp.getText().split(":");
                    lockOwner = lock[0];
                    lockCreated = lock[1];
                }
            } else if ("lockCreated".equals(key)) {
                lockCreated = jp.getText();
            } else if ("lockOwner".equals(key)) {
                lockOwner = jp.getText();
            } else if ("repository".equals(key)) {
                repository = jp.getText();
            } else if ("properties".equals(key)) {
                readProperties(jp, properties);
            } else if ("changeToken".equals(key)) {
                changeToken = jp.getText();
            }
        }
        return new Document(uid, type, null, changeToken, path, state, lockOwner, lockCreated, repository,
                versionLabel, isCheckedOut, properties, null);
    }

    protected static void readProperties(JsonParser jp, Map<String, Object> props) throws IOException {
        JsonToken tok = jp.nextToken();
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            if (tok == JsonToken.START_ARRAY) {
                props.put(key, readArrayProperty(jp));
            } else if (tok == JsonToken.START_OBJECT) {
                props.put(key, readObjectProperty(jp));
            } else if (tok == JsonToken.VALUE_NULL) {
                props.put(key, (String) null);
            } else {
                props.put(key, jp.getText());
            }
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
    }

    protected static Map<String, Object> readObjectProperty(JsonParser jp) throws IOException {
        Map<String, Object> map = new HashMap<>();
        readProperties(jp, map);
        return map;
    }

    protected static List<Object> readArrayProperty(JsonParser jp) throws IOException {
        List<Object> list = new ArrayList<>();
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            if (tok == JsonToken.START_ARRAY) {
                list.add(readArrayProperty(jp));
            } else if (tok == JsonToken.START_OBJECT) {
                list.add(readObjectProperty(jp));
            } else {
                list.add(jp.getText());
            }
            tok = jp.nextToken();
        }
        return list;
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

}

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

import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;
import org.nuxeo.ecm.automation.client.model.PropertyMapSetter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * @author matic
 */
public class DocumentMarshaller implements JsonMarshaller<Document> {

    @Override
    public String getType() {
        return "document";
    }

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
        PropertyList facets = null;
        String changeToken = null;
        JsonToken tok = jp.nextToken();
        PropertyMap props = new PropertyMap();
        PropertyMapSetter propsSetter = new PropertyMapSetter(props);
        PropertyMap contextParameters = new PropertyMap();
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            if (key.equals("uid")) {
                uid = jp.getText();
            } else if (key.equals("path")) {
                path = jp.getText();
            } else if (key.equals("type")) {
                type = jp.getText();
            } else if (key.equals("state")) {
                state = jp.getText();
            } else if (key.equals("versionLabel")) {
                versionLabel = jp.getText();
            } else if (key.equals("isCheckedOut")) {
                isCheckedOut = jp.getText();
            } else if (key.equals("lock")) {
                if (!JsonToken.VALUE_NULL.equals(jp.getCurrentToken())) {
                    String[] lock = jp.getText().split(":");
                    lockOwner = lock[0];
                    lockCreated = lock[1];
                }
            } else if (key.equals("lockCreated")) {
                lockCreated = jp.getText();
            } else if (key.equals("lockOwner")) {
                lockOwner = jp.getText();
            } else if (key.equals("repository")) {
                repository = jp.getText();
            } else if (key.equals("title")) {
                propsSetter.set("dc:title", jp.getText());
            } else if (key.equals("lastModified")) {
                propsSetter.set("dc:modified", jp.getText());
            } else if (key.equals("properties")) {
                readProperties(jp, props);
            } else if (key.equals("facets")) {
                facets = readArrayProperty(jp);
            } else if (key.equals("changeToken")) {
                changeToken = jp.getText();
            } else if (key.equals("contextParameters")) {
                readProperties(jp, contextParameters);
            } else {
                // do skip unknown keys
                jp.skipChildren();
            }
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
        return new Document(uid, type, facets, changeToken, path, state, lockOwner, lockCreated, repository,
                versionLabel, isCheckedOut, props, contextParameters);
    }

    protected static void readProperties(JsonParser jp, PropertyMap props) throws IOException {
        PropertyMapSetter setter = new PropertyMapSetter(props);
        JsonToken tok = jp.nextToken();
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            if (tok == JsonToken.START_ARRAY) {
                setter.set(key, readArrayProperty(jp));
            } else if (tok == JsonToken.START_OBJECT) {
                setter.set(key, readObjectProperty(jp));
            } else if (tok == JsonToken.VALUE_NULL) {
                setter.set(key, (String) null);
            } else {
                setter.set(key, jp.getText());
            }
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
    }

    protected static PropertyMap readObjectProperty(JsonParser jp) throws IOException {
        PropertyMap map = new PropertyMap();
        readProperties(jp, map);
        return map;
    }

    protected static PropertyList readArrayProperty(JsonParser jp) throws IOException {
        PropertyList list = new PropertyList();
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
        // TODO: extend the server json API to allow for document refs passed as
        // JSON data-structures instead of the input ref microsyntax used by
        throw new UnsupportedOperationException();
    }

}

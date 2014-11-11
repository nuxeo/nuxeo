/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;

/**
 * @author matic
 *
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
    public Document read(JsonParser jp) throws Exception {
        return readDocument(jp);
    }

    protected static Document readDocument(JsonParser jp) throws Exception {
        String uid = null;
        String type = null;
        String path = null;
        String state = null;
        String versionLabel = null;
        String lockCreated = null;
        String lockOwner = null;
        String repository = null;
        PropertyList facets = null;
        String changeToken = null;
        JsonToken tok = jp.nextToken();
        PropertyMap props = new PropertyMap();
        PropertyMap contextParameters = new PropertyMap();
        while (tok != JsonToken.END_OBJECT) {
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
            }else if (key.equals("repository")) {
                repository = jp.getText();
            } else if (key.equals("title")) {
                props.set("dc:title", jp.getText());
            } else if (key.equals("lastModified")) {
                props.set("dc:modified", jp.getText());
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
        return new Document(uid, type, facets, changeToken, path, state, lockOwner, lockCreated, repository, versionLabel, props, null);
    }

    protected static void readProperties(JsonParser jp, PropertyMap props) throws Exception {
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            if (tok == JsonToken.START_ARRAY) {
                props.set(key, readArrayProperty(jp));
            } else if (tok == JsonToken.START_OBJECT) {
                props.set(key, readObjectProperty(jp));
            } else if (tok  == JsonToken.VALUE_NULL) {
                props.set(key, (String)null);
            } else {
                props.set(key, jp.getText());
            }
            tok = jp.nextToken();
        }
    }

    protected static PropertyMap readObjectProperty(JsonParser jp) throws Exception {
        PropertyMap map = new PropertyMap();
        readProperties(jp, map);
        return map;
    }

    protected static PropertyList readArrayProperty(JsonParser jp) throws Exception {
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
    public void write(JsonGenerator jg, Document value) throws Exception {
        throw new UnsupportedOperationException();
    }

}

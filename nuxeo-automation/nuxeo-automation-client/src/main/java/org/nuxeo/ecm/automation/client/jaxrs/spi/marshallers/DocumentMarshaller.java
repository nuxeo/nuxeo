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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyList;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

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
    public String getReference(Document info) {
        return info.getInputRef();
    }

    @Override
    public Document read(JSONObject json) {
        return readDocument(json);
    }
    
    
    protected static Document readDocument(JSONObject json) {
        String uid = json.getString("uid");
        String path = json.getString("path");
        String type = json.getString("type");
        String state = json.optString("state", null);
        String lock = json.optString("lock", null);
        String title = json.optString("title", null);
        String lastModified = json.optString("lastModified", null);
        JSONObject jsonProps = json.optJSONObject("properties");
        PropertyMap props;
        if (jsonProps != null) {
            props = (PropertyMap) readValue(jsonProps);
        } else {
            props = new PropertyMap();
        }
        props.set("dc:title", title);
        props.set("dc:modified", lastModified);
        return new Document(uid, type, path, state, lock, props);
    }
    

    @Override
    public void write(JSONObject object, Document doc) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    protected static Object readValue(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof JSON) {
            JSON jo = (JSON) o;
            if (jo == JSONNull.getInstance()) {
                return null;
            } else if (jo.isArray()) {
                JSONArray ar = (JSONArray) jo;
                PropertyList plist = new PropertyList();
                List<Object> list = plist.list();
                for (int i = 0, size = ar.size(); i < size; i++) {
                    Object v = readValue(ar.get(i));
                    if (v != null) {
                        list.add(v);
                    }
                }
                return plist;
            } else {
                JSONObject ob = (JSONObject) jo;
                if (ob.isNullObject()) {
                    return null;
                }
                PropertyMap pmap = new PropertyMap();
                Map<String, Object> map = pmap.map();
                Iterator<String> keys = ob.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object v = readValue(ob.get(key));
                    map.put(key, v);
                }
                return pmap;
            }
        } else {
            return o.toString();
        }
    }

}

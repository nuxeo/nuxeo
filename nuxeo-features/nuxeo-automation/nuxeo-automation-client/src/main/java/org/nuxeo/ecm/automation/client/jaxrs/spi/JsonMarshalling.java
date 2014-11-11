/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.LoginInfo;
import org.nuxeo.ecm.automation.client.jaxrs.OperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationInput;
import org.nuxeo.ecm.automation.client.jaxrs.model.PaginableDocuments;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyList;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;
import org.nuxeo.ecm.automation.client.jaxrs.util.JSONExporter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JsonMarshalling {
	
	private JsonMarshalling() {
	}

    @SuppressWarnings("unchecked")
    public static OperationRegistry readRegistry(String content) {
        JSONObject json = JSONObject.fromObject(content);
        HashMap<String, OperationDocumentation> ops = new HashMap<String, OperationDocumentation>();
        HashMap<String, OperationDocumentation> chains = new HashMap<String, OperationDocumentation>();
        HashMap<String, String> paths = new HashMap<String, String>();
        JSONArray ar = json.getJSONArray("operations");
        if (ar != null) {
            for (int i = 0, len = ar.size(); i < len; i++) {
                JSONObject obj = ar.getJSONObject(i);
                OperationDocumentation op = JSONExporter.fromJSON(obj);
                ops.put(op.id, op);
            }
        }
        ar = json.getJSONArray("chains");
        if (ar != null) {
            for (int i = 0, len = ar.size(); i < len; i++) {
                JSONObject obj = ar.getJSONObject(i);
                OperationDocumentation op = JSONExporter.fromJSON(obj);
                chains.put(op.id, op);
            }
        }
        JSONObject pathsObj = json.getJSONObject("paths");
        if (pathsObj != null) {
            Iterator<String> it = pathsObj.keys();
            while (it.hasNext()) {
                String key = it.next();
                String value = pathsObj.getString(key);
                paths.put(key, value);
            }
        }
        return new OperationRegistry(paths, ops, chains);
    }

    public static Object readEntity(String content) {
        if (content.length() == 0) { // void response
            return null;
        }
        JSONObject json = JSONObject.fromObject(content);
        String type = json.getString(Constants.KEY_ENTITY_TYPE);
        if ("document".equals(type)) {
            return readDocument(json);
        } else if ("documents".equals(type)) {
            Documents docs;
            JSONArray ar = json.getJSONArray("entries");
            int size = ar.size();
            if (json.optBoolean("isPaginable") == true) {
                int totalSize = json.getInt("totalSize");
                int pageSize = json.getInt("pageSize");
                int pageCount = json.getInt("pageCount");
                int pageIndex = json.getInt("pageIndex");
                docs = new PaginableDocuments(size, totalSize, pageSize, pageCount, pageIndex);
            } else {
                docs = new Documents(size);
            }
            for (int i = 0; i < size; i++) {
                JSONObject obj = ar.getJSONObject(i);
                docs.add(readDocument(obj));
            }
            return docs;
        } else if ("login".equals(type)) {
            return readLogin(json);
        } else if ("exception".equals(type)) {
            throw readException(content);
        }
        throw new IllegalArgumentException("Unknown entity type: " + type);
    }

    public static RemoteException readException(String content) {
        return readException(JSONObject.fromObject(content));
    }

    protected static RemoteException readException(JSONObject json) {
        return new RemoteException(Integer.parseInt(json.getString("status")),
                json.optString("type", null), json.optString("message"),
                json.optString("stack", null));
    }

    protected static LoginInfo readLogin(JSONObject json) {
        String username = json.getString("username");
        String isAdmin = json.optString("isAdministrator", "false");
        JSONArray groups = json.optJSONArray("groups");
        HashSet<String> set = new HashSet<String>();
        if (groups != null) {
            for (int i = 0, size = groups.size(); i < size; i++) {
                set.add(groups.getString(i));
            }
        }
        return new LoginInfo(username, set, Boolean.parseBoolean(isAdmin));
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

    public static String writeRequest(OperationRequest req) throws Exception {
        JSONObject entity = new JSONObject();
        OperationInput input = req.getInput();

        if (input != null && !input.isBinary()) {
            String ref = input.getInputRef();
            if (ref != null) {
                entity.element("input", ref);
            }
        }
        entity.element("params", req.getParameters());
        entity.element("context", req.getContextParameters());
        return entity.toString();
    }

}

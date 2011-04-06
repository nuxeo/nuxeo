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
import java.util.Iterator;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.OperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationInput;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.DateMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.DocumentMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.DocumentsMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.ExceptionMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.LoginMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.PrimitiveMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.util.JSONExporter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JsonMarshalling {
	
	private JsonMarshalling() {
	}

	protected static final HashMap<String,JsonMarshaller<?>> marshallersByType = 
	        new HashMap<String,JsonMarshaller<?>>();
	
	protected static final HashMap<Class<?>,JsonMarshaller<?>> marshallersByJavaType = 
	        new HashMap<Class<?>,JsonMarshaller<?>>();
	
	static {
	    addMarshaller(new DocumentMarshaller());
	    addMarshaller(new DocumentsMarshaller());
	    addMarshaller(new ExceptionMarshaller());
	    addMarshaller(new LoginMarshaller());
	    addMarshaller(new DateMarshaller());
	    addMarshaller(new PrimitiveMarshaller<String>(String.class));
	    addMarshaller(new PrimitiveMarshaller<Boolean>(Boolean.class));
	    addMarshaller(new PrimitiveMarshaller<Integer>(Integer.class));
		addMarshaller(new PrimitiveMarshaller<Long>(Long.class));
		addMarshaller(new PrimitiveMarshaller<Double>(Double.class));

	}
	
	public static void addMarshaller(JsonMarshaller<?> marshaller) {
	    marshallersByType.put(marshaller.getType(), marshaller);
	    marshallersByJavaType.put(marshaller.getJavaType(), marshaller);
	}
	
	@SuppressWarnings("unchecked")
    public static <T> JsonMarshaller<T> getMarshaller(String type) {
	    return (JsonMarshaller<T>)marshallersByType.get(type);
	}
	
	@SuppressWarnings("unchecked")
    public static <T> JsonMarshaller<T> getMarshaller(Class<T> clazz) {
	    return (JsonMarshaller<T>)marshallersByJavaType.get(clazz);
	}

	protected static JsonMarshaller<?> findMarshaller(JSONObject json) {
	    String type= json.getString(Constants.KEY_ENTITY_TYPE);
	    JsonMarshaller<?> js = marshallersByType.get(type);
	    if (js == null) {
	        throw new IllegalArgumentException("no marshaller for " + type);
	    }
	    return js;
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
        JsonMarshaller<?> marshaller = findMarshaller(json);
        return marshaller.read(json);
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

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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationInput;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.DocumentMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.DocumentsMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.ExceptionMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.LoginMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.util.JsonOperationMarshaller;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JsonMarshalling {

	private JsonMarshalling() {
	}

	protected static JsonFactory factory = new JsonFactory();

	protected static final HashMap<String,JsonMarshaller<?>> marshallersByType =
	        new HashMap<String,JsonMarshaller<?>>();

	protected static final HashMap<Class<?>,JsonMarshaller<?>> marshallersByJavaType =
	        new HashMap<Class<?>,JsonMarshaller<?>>();

	public static JsonFactory getFactory() {
	    return factory;
	}

	static {
	    addMarshaller(new DocumentMarshaller());
	    addMarshaller(new DocumentsMarshaller());
	    addMarshaller(new ExceptionMarshaller());
	    addMarshaller(new LoginMarshaller());
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

    public static OperationRegistry readRegistry(String content) throws Exception {
        HashMap<String, OperationDocumentation> ops = new HashMap<String, OperationDocumentation>();
        HashMap<String, OperationDocumentation> chains = new HashMap<String, OperationDocumentation>();
        HashMap<String, String> paths = new HashMap<String, String>();

        JsonParser jp = factory.createJsonParser(content);
        jp.nextToken(); // start_obj
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            if ("operations".equals(key)) {
                readOperations(jp, ops);
            } else if ("chains".equals(key)) {
                readChains(jp, chains);
            } else if ("paths".equals(key)) {
                readPaths(jp, paths);
            }
            tok = jp.nextToken();
        }
        return new OperationRegistry(paths, ops, chains);
    }

    private static void readOperations(JsonParser jp, Map<String, OperationDocumentation> ops) throws Exception {
        jp.nextToken(); // skip [
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            OperationDocumentation op = JsonOperationMarshaller.read(jp);
            ops.put(op.id, op);
            tok = jp.nextToken();
        }
    }

    private static void readChains(JsonParser jp, Map<String, OperationDocumentation> chains) throws Exception {
        jp.nextToken(); // skip [
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            OperationDocumentation op = JsonOperationMarshaller.read(jp);
            chains.put(op.id, op);
            tok = jp.nextToken();
        }
    }

    private static void readPaths(JsonParser jp, Map<String, String> paths) throws Exception {
        jp.nextToken(); // skip {
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_OBJECT) {
            jp.nextToken();
            paths.put(jp.getCurrentName(), jp.getText());
            tok = jp.nextToken();
        }
    }

    public static Object readEntity(String content) throws Exception {
        if (content.length() == 0) { // void response
            return null;
        }
        JsonParser jp = factory.createJsonParser(content);
        jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
        jp.nextToken();
        if (!Constants.KEY_ENTITY_TYPE.equals(jp.getText())) {
            throw new RuntimeException("unuspported respone type. No entity-type key found at top of the object");
        }
        jp.nextToken();
        String etype = jp.getText();
        JsonMarshaller<?> jm = marshallersByType.get(etype);
        if (jm == null) {
            throw new IllegalArgumentException("no marshaller for " + etype);
        }
        return jm.read(jp);
    }

    public static String writeRequest(OperationRequest req) throws Exception {
        StringWriter writer = new StringWriter();
        OperationInput input = req.getInput();
        JsonGenerator jg = factory.createJsonGenerator(writer);
        jg.writeStartObject();
        if (input != null && !input.isBinary()) {
            String ref = input.getInputRef();
            if (ref != null) {
                jg.writeStringField("input", ref);
            }
        }
        jg.writeObjectFieldStart("params");
        writeMap(jg, req.getParameters());
        jg.writeEndObject();
        jg.writeObjectFieldStart("context");
        writeMap(jg, req.getContextParameters());
        jg.writeEndObject();
        jg.writeEndObject();
        jg.close();
        return writer.toString();
    }

    public static void writeMap(JsonGenerator jg, Map<String,Object> map) throws Exception {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object obj = entry.getValue();
            if (obj.getClass() == String.class) {
                jg.writeStringField(entry.getKey(), (String)entry.getValue());
            } else {
                throw new UnsupportedOperationException("Not yet implemented"); //TODO
            }
        }
    }

}

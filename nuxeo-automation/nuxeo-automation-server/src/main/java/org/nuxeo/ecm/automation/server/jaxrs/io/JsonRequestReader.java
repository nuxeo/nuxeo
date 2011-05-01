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
package org.nuxeo.ecm.automation.server.jaxrs.io;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.server.jaxrs.ExecutionRequest;
import org.nuxeo.ecm.automation.server.jaxrs.io.resolvers.DocumentInputResolver;
import org.nuxeo.ecm.automation.server.jaxrs.io.resolvers.DocumentsInputResolver;
import org.nuxeo.ecm.automation.server.jaxrs.io.writers.JsonDocumentListWriter;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Consumes("application/json+nxrequest")
public class JsonRequestReader implements MessageBodyReader<ExecutionRequest> {

    public static final MediaType targetMediaType = new MediaType("application", "json+nxrequest");

    protected static JsonMarshalling marshalling() {
        return Framework.getLocalService(JsonMarshalling.class);
    }

    protected static final HashMap<String,InputResolver<?>> inputResolvers =
            new HashMap<String,InputResolver<?>>();

    static {
        addInputResolver(new DocumentInputResolver());
        addInputResolver(new DocumentsInputResolver());
    }

    public static void addInputResolver(InputResolver<?> resolver) {
        inputResolvers.put(resolver.getType(), resolver);
    }

    public static Object resolveInput(String input) {
        int p = input.indexOf(':');
        if (p <= 0) {
            throw new IllegalArgumentException(input + " is not formatted using type:value");
        }
        String type = input.substring(0,p);
        String ref = input.substring(p+1);
        InputResolver<?> ir = inputResolvers.get(type);
        if (ir != null) {
            return ir.getInput(ref);
        }
        JsonMarshaller<Object> marshaller = marshalling().getMarshaller(type);
        if (marshaller == null) {
            throw new IllegalArgumentException("Cannot find resolver for " + type);
        }
        return marshaller.resolveReference(ref);
    }

    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return (targetMediaType.isCompatible(arg3) && ExecutionRequest.class.isAssignableFrom(arg0));
    }

    public ExecutionRequest readFrom(Class<ExecutionRequest> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3,
            MultivaluedMap<String, String> headers, InputStream in)
            throws IOException, WebApplicationException {
        return readRequest(in,headers);
    }

    public static ExecutionRequest readRequest(InputStream in, MultivaluedMap<String, String> headers)
            throws IOException {
        String content = FileUtils.read(in);
        return readRequest(content, headers);
    }

    @SuppressWarnings("unchecked")
    public static ExecutionRequest readRequest(String content, MultivaluedMap<String, String> headers) {
        JSONObject json = JSONObject.fromObject(content);
        String input = json.optString("input", null);
        JSONObject jsonParams = json.optJSONObject("params");
        JSONObject jsonContext = json.optJSONObject("context");

        String documentProperties = json.optString("documentProperties", null);
        if (documentProperties!=null) {
            headers.putSingle(JsonDocumentListWriter.DOCUMENT_PROPERTIES_HEADER, documentProperties);
        }

        Object inObj = null;
        if (input != null) {
            inObj= resolveInput(input);
        }
        ExecutionRequest req = new ExecutionRequest(inObj);

        if (jsonParams != null) {
            Iterator<String> it = jsonParams.keys();
            while (it.hasNext()) {
                String key = it.next();
                JSONObject object = jsonParams.optJSONObject(key);
                if (object!=null) {
                    req.setParam(key, object);
                }else {
                    String value = jsonParams.getString(key);
                    req.setParam(key, value);
                }
            }
        }

        if (jsonContext != null) {
            Iterator<String> it = jsonContext.keys();
            while (it.hasNext()) {
                String key = it.next();
                JSONObject object = jsonContext.optJSONObject(key);
                if (object!=null) {
                    req.setContextParam(key, object);
                } else {
                    String value = jsonContext.getString(key);
                    req.setContextParam(key, value);
                }
            }
        }

        return req;
    }


}

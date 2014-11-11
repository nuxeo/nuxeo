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
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Consumes({ "application/json", "application/json+nxrequest" })
public class JsonRequestReader implements MessageBodyReader<ExecutionRequest> {

    @Context
    private HttpServletRequest request;

    @Context
    JsonFactory factory;

    public CoreSession getCoreSession() {
        return SessionFactory.getSession(request);
    }

    public static final MediaType targetMediaTypeNXReq = new MediaType(
            "application", "json+nxrequest");

    public static final MediaType targetMediaType = new MediaType(
            "application", "json");

    protected static final HashMap<String, InputResolver<?>> inputResolvers = new HashMap<String, InputResolver<?>>();

    static {
        addInputResolver(new DocumentInputResolver());
        addInputResolver(new DocumentsInputResolver());
    }

    public static void addInputResolver(InputResolver<?> resolver) {
        inputResolvers.put(resolver.getType(), resolver);
    }

    public static Object resolveInput(String input) throws Exception {
        int p = input.indexOf(':');
        if (p <= 0) {
            // pass the String object directly
            return input;
        }
        String type = input.substring(0, p);
        String ref = input.substring(p + 1);
        InputResolver<?> ir = inputResolvers.get(type);
        if (ir != null) {
            return ir.getInput(ref);
        }
        // no resolver found, pass the String object directly.
        return input;
    }

    @Override
    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return ((targetMediaTypeNXReq.isCompatible(arg3) || targetMediaType
                .isCompatible(arg3)) && ExecutionRequest.class
                .isAssignableFrom(arg0));
    }

    @Override
    public ExecutionRequest readFrom(Class<ExecutionRequest> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3,
            MultivaluedMap<String, String> headers, InputStream in)
            throws IOException, WebApplicationException {
        return readRequest(in, headers, getCoreSession());
    }

    public ExecutionRequest readRequest(InputStream in,
            MultivaluedMap<String, String> headers, CoreSession session)
            throws IOException, WebApplicationException {
        // As stated in http://tools.ietf.org/html/rfc4627.html UTF-8 is the
        // default encoding for JSON content
        // TODO: add introspection on the first bytes to detect other admissible
        // json encodings, namely: UTF-8, UTF-16 (BE or LE), or UTF-32 (BE or
        // LE)
        String content = IOUtils.toString(in, "UTF-8");
        if (content.isEmpty()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return readRequest(content, headers, session);
    }

    public ExecutionRequest readRequest(String content,
            MultivaluedMap<String, String> headers, CoreSession session)
            throws WebApplicationException {
        try {
            return readRequest0(content, headers, session);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    public ExecutionRequest readRequest0(String content,
            MultivaluedMap<String, String> headers, CoreSession session)
            throws Exception {

        JsonParser jp = factory.createJsonParser(content);

        return readRequest(jp, headers, session);
    }

    /**
     * @param jp
     * @param headers
     * @param session
     * @return
     * @throws Exception
     * @since TODO
     */
    public static ExecutionRequest readRequest(JsonParser jp,
            MultivaluedMap<String, String> headers, CoreSession session) throws Exception {
        ExecutionRequest req = new ExecutionRequest();

        ObjectCodecService codecService = Framework.getLocalService(ObjectCodecService.class);
        jp.nextToken(); // skip {
        JsonToken tok = jp.nextToken();
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("input".equals(key)) {
                JsonNode inputNode = jp.readValueAsTree();
                if (inputNode.isTextual()) {
                    // string values are expected to be micro-parsed with
                    // the "type:value" syntax for backward compatibility
                    // reasons.
                    req.setInput(resolveInput(inputNode.getTextValue()));
                } else {
                    req.setInput(codecService.readNode(inputNode, session));
                }
            } else if ("params".equals(key)) {
                readParams(jp, req, session);
            } else if ("context".equals(key)) {
                readContext(jp, req, session);
            } else if ("documentProperties".equals(key)) {
                // TODO XXX - this is wrong - headers are ready only! see with
                // td
                String documentProperties = jp.getText();
                if (documentProperties != null) {
                    headers.putSingle(
                            JsonDocumentWriter.DOCUMENT_PROPERTIES_HEADER,
                            documentProperties);
                }
            }
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException(
                    "Unexpected end of stream.");
        }
        return req;
    }

    private static void readParams(JsonParser jp, ExecutionRequest req,
            CoreSession session) throws Exception {
        ObjectCodecService codecService = Framework.getLocalService(ObjectCodecService.class);
        JsonToken tok = jp.nextToken(); // move to first entry
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            req.setParam(key,
                    codecService.readNode(jp.readValueAsTree(), session));
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException(
                    "Unexpected end of stream.");
        }
    }

    private static void readContext(JsonParser jp, ExecutionRequest req,
            CoreSession session) throws Exception {
        ObjectCodecService codecService = Framework.getLocalService(ObjectCodecService.class);
        JsonToken tok = jp.nextToken(); // move to first entry
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            req.setContextParam(key,
                    codecService.readNode(jp.readValueAsTree(), session));
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException(
                    "Unexpected end of stream.");
        }
    }

}

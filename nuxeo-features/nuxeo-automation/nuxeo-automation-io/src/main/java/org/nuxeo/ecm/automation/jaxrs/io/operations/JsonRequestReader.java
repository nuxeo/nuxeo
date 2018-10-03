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
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxrequest" })
public class JsonRequestReader implements MessageBodyReader<ExecutionRequest> {

    @Context
    private HttpServletRequest request;

    @Context
    JsonFactory factory;

    public CoreSession getCoreSession() {
        return SessionFactory.getSession(request);
    }

    /**
     * @deprecated since 10.3. only 'application/json' media type should be used.
     */
    @Deprecated
    public static final MediaType targetMediaTypeNXReq = new MediaType("application", "json+nxrequest");

    protected static final HashMap<String, InputResolver<?>> inputResolvers = new HashMap<String, InputResolver<?>>();

    static {
        addInputResolver(new DocumentInputResolver());
        addInputResolver(new DocumentsInputResolver());
        addInputResolver(new BlobInputResolver());
        addInputResolver(new BlobsInputResolver());
    }

    public static void addInputResolver(InputResolver<?> resolver) {
        inputResolvers.put(resolver.getType(), resolver);
    }

    public static Object resolveInput(String input) throws IOException {
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
    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return ((targetMediaTypeNXReq.isCompatible(arg3) || MediaType.APPLICATION_JSON_TYPE.isCompatible(arg3))
                && ExecutionRequest.class.isAssignableFrom(arg0));
    }

    @Override
    public ExecutionRequest readFrom(Class<ExecutionRequest> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
            MultivaluedMap<String, String> headers, InputStream in) throws IOException, WebApplicationException {
        return readRequest(in, headers, getCoreSession());
    }

    public ExecutionRequest readRequest(InputStream in, MultivaluedMap<String, String> headers, CoreSession session)
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

    public ExecutionRequest readRequest(String content, MultivaluedMap<String, String> headers, CoreSession session)
            throws WebApplicationException {
        try {
            return readRequest0(content, headers, session);
        } catch (WebApplicationException e) {
            throw e;
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }

    public ExecutionRequest readRequest0(String content, MultivaluedMap<String, String> headers, CoreSession session)
            throws IOException {

        JsonParser jp = factory.createJsonParser(content);

        return readRequest(jp, headers, session);
    }

    /**
     * @param jp
     * @param headers
     * @param session
     * @return
     * @since TODO
     */
    public static ExecutionRequest readRequest(JsonParser jp, MultivaluedMap<String, String> headers,
            CoreSession session) throws IOException {
        ExecutionRequest req = new ExecutionRequest();

        ObjectCodecService codecService = Framework.getService(ObjectCodecService.class);
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
                    req.setInput(resolveInput(inputNode.textValue()));
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
                    headers.putSingle(MarshallingConstants.EMBED_PROPERTIES, documentProperties);
                }
            }
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
        return req;
    }

    private static void readParams(JsonParser jp, ExecutionRequest req, CoreSession session) throws IOException {
        ObjectCodecService codecService = Framework.getService(ObjectCodecService.class);
        JsonToken tok = jp.nextToken(); // move to first entry
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            req.setParam(key, codecService.readNode(jp.readValueAsTree(), session));
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
    }

    private static void readContext(JsonParser jp, ExecutionRequest req, CoreSession session) throws IOException {
        ObjectCodecService codecService = Framework.getService(ObjectCodecService.class);
        JsonToken tok = jp.nextToken(); // move to first entry
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            tok = jp.nextToken();
            req.setContextParam(key, codecService.readNode(jp.readValueAsTree(), session));
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
    }

}

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
package org.nuxeo.ecm.restapi.server.jaxrs.routing.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.node.ArrayNode;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.io.util.JsonEncodeDecodeUtils;
import org.nuxeo.ecm.restapi.server.jaxrs.routing.model.WorkflowRequest;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;

/**
 * @author <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
@Provider
public class WorkflowRequestReader implements MessageBodyReader<WorkflowRequest> {

    protected static final Log log = LogFactory.getLog(WorkflowRequestReader.class);

    public static final String ENTITY_TYPE = "workflow";

    @Context
    private JsonFactory factory;

    @Context
    HttpServletRequest request;

    @Context
    UriInfo uriInfo;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return WorkflowRequest.class.isAssignableFrom(type);
    }

    @Override
    public WorkflowRequest readFrom(Class<WorkflowRequest> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        String content = IOUtils.toString(entityStream);
        if (content.isEmpty()) {
            if (content.isEmpty()) {
                throw new WebException("No content in request body", Response.Status.BAD_REQUEST.getStatusCode());
            }

        }

        try {
            return readRequest(content, httpHeaders);
        } catch (IOException | NuxeoException | ClassNotFoundException e) {
            throw WebException.wrap(e);
        }
    }

    protected WorkflowRequest readRequest(String content, MultivaluedMap<String, String> httpHeaders)
            throws JsonParseException, IOException, ClassNotFoundException {
        CoreSession session = SessionFactory.getSession(request);
        JsonParser jp = factory.createJsonParser(content);
        WorkflowRequest workflowRequest = new WorkflowRequest();

        JsonToken tok = jp.nextToken();

        // skip {
        if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        }
        String workflowModelName = null;
        List<String> attachedDocumentIds = null;
        JsonNode variableNode = null;
        Map<String, Serializable> variables = null;
        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("workflowModelName".equals(key)) {
                workflowModelName = jp.readValueAs(String.class);
            } else if ("attachedDocumentIds".equals(key)) {
                ArrayNode docNodes = (ArrayNode) jp.readValueAsTree();
                attachedDocumentIds = new ArrayList<String>();
                for (int i = 0; i < docNodes.size(); i++) {
                    JsonNode node = docNodes.get(i);
                    attachedDocumentIds.add(node.getTextValue());
                }
            } else if ("variables".equals(key)) {
                variableNode = jp.readValueAsTree();
            } else if ("entity-type".equals(key)) {
                String entityType = jp.readValueAs(String.class);
                if (!WorkflowRequestReader.ENTITY_TYPE.equals(entityType)) {
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
            } else {
                log.debug("Unknown key: " + key);
                jp.skipChildren();
            }
            tok = jp.nextToken();

        }

        if (workflowModelName == null) {
            throw new WebException("No workflowModelName found in request body",
                    Response.Status.BAD_REQUEST.getStatusCode());
        }

        if (variableNode != null) {
            variables = JsonEncodeDecodeUtils.decodeVariables(variableNode, null, session);
        }
        workflowRequest.setWorkflowModelName(workflowModelName);
        workflowRequest.setVariables(variables);
        workflowRequest.setAttachedDocumentIds(attachedDocumentIds);

        return workflowRequest;
    }

}

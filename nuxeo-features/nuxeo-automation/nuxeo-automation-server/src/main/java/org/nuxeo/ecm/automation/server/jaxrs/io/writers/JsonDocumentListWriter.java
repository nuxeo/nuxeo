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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.automation.server.jaxrs.io.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonWriter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class JsonDocumentListWriter implements
        MessageBodyWriter<DocumentModelList> {

    private static final Log log = LogFactory.getLog(JsonDocumentListWriter.class);

    @Context
    protected HttpHeaders headers;

    public long getSize(DocumentModelList arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return DocumentModelList.class.isAssignableFrom(arg0);
    }

    public void writeTo(DocumentModelList docs, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4,
            MultivaluedMap<String, Object> arg5, OutputStream out)
            throws IOException, WebApplicationException {
        try {
            List<String> props = headers.getRequestHeader(JsonDocumentWriter.DOCUMENT_PROPERTIES_HEADER);
            String[] schemas = null;
            if (props != null && !props.isEmpty()) {
                schemas = StringUtils.split(props.get(0), ',', true);
            }
            writeDocuments(out, docs, schemas);
        } catch (Exception e) {
            log.error("Failed to wserialize document list", e);
            throw new WebApplicationException(500);
        }
    }

    public static void writeDocuments(OutputStream out, DocumentModelList docs, String[] schemas)
            throws Exception {
        writeDocuments(JsonWriter.createGenerator(out), docs, schemas);
    }

    public static void writeDocuments(JsonGenerator jg, DocumentModelList docs, String[] schemas)
            throws Exception {
        jg.writeStartObject();
        jg.writeStringField("entity-type", "documents");
        if (docs instanceof PaginableDocumentModelList) {
            PaginableDocumentModelList provider = (PaginableDocumentModelList) docs;
            jg.writeBooleanField("isPaginable", true);
            jg.writeNumberField("totalSize", provider.totalSize());
            jg.writeNumberField("pageIndex", provider.getCurrentPageIndex());
            jg.writeNumberField("pageSize", provider.getPageSize());
            jg.writeNumberField("pageCount", provider.getNumberOfPages());
        }
        jg.writeArrayFieldStart("entries");
        for (DocumentModel doc : docs) {
            JsonDocumentWriter.writeDocument(jg, doc, schemas);
        }
        jg.writeEndArray();
        jg.writeEndObject();
        jg.flush();
    }

}

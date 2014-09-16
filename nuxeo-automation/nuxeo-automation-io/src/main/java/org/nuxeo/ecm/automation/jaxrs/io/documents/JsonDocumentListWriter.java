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
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList;
import org.nuxeo.ecm.automation.jaxrs.io.EntityListWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateQuery;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class JsonDocumentListWriter extends EntityListWriter<DocumentModel> {

    private static final Log log = LogFactory.getLog(JsonDocumentListWriter.class);

    @Context
    JsonFactory factory;

    @Context
    protected HttpHeaders headers;

    @Context
    protected HttpServletRequest request;

    @Override
    protected String getEntityType() {
        return "documents";
    }

    @Override
    protected void writeItem(JsonGenerator jg, DocumentModel item)
            throws ClientException, IOException {
        // do nothing, everything is done in #writeTo
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return DocumentModelList.class.isAssignableFrom(arg0);
    }

    @Override
    public void writeTo(List<DocumentModel> docs, Class<?> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
        try {
            List<String> props = headers.getRequestHeader(JsonDocumentWriter.DOCUMENT_PROPERTIES_HEADER);
            String[] schemas = null;
            if (props != null && !props.isEmpty()) {
                schemas = StringUtils.split(props.get(0), ',', true);
            }
            writeDocuments(entityStream, docs, schemas, headers);
        } catch (Exception e) {
            log.error("Failed to serialize document list", e);
            throw new WebApplicationException(500);
        }
    }

    public void writeDocuments(OutputStream out, List<DocumentModel> docs,
            String[] schemas) throws Exception {
        writeDocuments(factory.createJsonGenerator(out, JsonEncoding.UTF8),
                docs, schemas, request);
    }

    /**
     * @since 5.9.5
     */
    public void writeDocuments(OutputStream out, List<DocumentModel> docs,
            String[] schemas, HttpHeaders headers) throws Exception {
        writeDocuments(factory.createJsonGenerator(out, JsonEncoding.UTF8),
                docs, schemas, headers, request);
    }

    public static void writeDocuments(JsonGenerator jg,
            List<DocumentModel> docs, String[] schemas, ServletRequest request)
            throws Exception {
        writeDocuments(jg, docs, schemas, null, request);
    }

    /**
     * @since 5.9.5
     */
    public static void writeDocuments(JsonGenerator jg,
            List<DocumentModel> docs, String[] schemas, HttpHeaders headers,
            ServletRequest request) throws Exception {
        jg.writeStartObject();
        jg.writeStringField("entity-type", "documents");

        if (docs instanceof PaginableDocumentModelList) {
            PaginableDocumentModelList provider = (PaginableDocumentModelList) docs;
            jg.writeBooleanField("isPaginable", true);
            jg.writeNumberField("resultsCount", provider.getResultsCount());
            jg.writeNumberField("pageSize", provider.getPageSize());
            jg.writeNumberField("maxPageSize", provider.getMaxPageSize());
            jg.writeNumberField("currentPageSize",
                    provider.getCurrentPageSize());
            jg.writeNumberField("currentPageIndex",
                    provider.getCurrentPageIndex());
            jg.writeNumberField("numberOfPages", provider.getNumberOfPages());
            jg.writeBooleanField("isPreviousPageAvailable",
                    provider.isPreviousPageAvailable());
            jg.writeBooleanField("isNextPageAvailable",
                    provider.isNextPageAvailable());
            jg.writeBooleanField("isLastPageAvailable",
                    provider.isLastPageAvailable());
            jg.writeBooleanField("isSortable", provider.isSortable());
            jg.writeBooleanField("hasError", provider.hasError());
            jg.writeStringField("errorMessage", provider.getErrorMessage());

            // compat fields
            jg.writeNumberField("totalSize", provider.totalSize());
            jg.writeNumberField("pageIndex", provider.getCurrentPageIndex());
            jg.writeNumberField("pageCount", provider.getNumberOfPages());

            DocumentViewCodecManager documentViewCodecManager = Framework.getLocalService(DocumentViewCodecManager.class);
            String codecName = null;
            if (documentViewCodecManager == null) {
                log.warn("Service 'DocumentViewCodecManager' not available : documentUrl won't be generated");
            } else {
                String documentLinkBuilder = provider.getDocumentLinkBuilder();
                codecName = isBlank(documentLinkBuilder) ? documentViewCodecManager.getDefaultCodecName()
                        : documentLinkBuilder;
            }

            jg.writeArrayFieldStart("entries");
            for (DocumentModel doc : docs) {
                DocumentLocation docLoc = new DocumentLocationImpl(doc);
                Map<String, String> contextParameters = new HashMap<String, String>();
                if (documentViewCodecManager != null) {
                    DocumentView docView = new DocumentViewImpl(docLoc,
                            doc.getAdapter(TypeInfo.class).getDefaultView());
                    String documentURL = VirtualHostHelper.getContextPathProperty()
                            + "/"
                            + documentViewCodecManager.getUrlFromDocumentView(
                                    codecName, docView, false, null);
                    contextParameters.put("documentURL", documentURL);
                }
                JsonDocumentWriter.writeDocument(jg, doc, schemas,
                        contextParameters, headers, request);
            }
            jg.writeEndArray();
            if (provider.hasAggregateSupport() && provider.getAggregates() !=
                    null && !provider.getAggregates().isEmpty()) {
                jg.writeObjectField("aggregations", provider.getAggregates());
            }
        } else {
            jg.writeArrayFieldStart("entries");
            for (DocumentModel doc : docs) {
                JsonDocumentWriter.writeDocument(jg, doc, schemas,
                        new HashMap<String, String>(), headers, request);
            }
            jg.writeEndArray();
        }

        jg.writeEndObject();
        jg.flush();
    }

}

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
package org.nuxeo.ecm.automation.server.jaxrs.io;

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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class JsonDocumentListWriter implements
        MessageBodyWriter<DocumentModelList> {

    public static final String DOCUMENT_PROPERTIES_HEADER = "X-NXDocumentProperties";

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
            MultivaluedMap<String, Object> arg5, OutputStream arg6)
            throws IOException, WebApplicationException {
        try {
            JSONObject json = new JSONObject();
            JSONArray ar = new JSONArray();
            List<String> props = headers.getRequestHeader(DOCUMENT_PROPERTIES_HEADER);
            String[] schemas = null;
            if (props != null && !props.isEmpty()) {
                schemas = StringUtils.split(props.get(0), ',', true);
            }
            for (DocumentModel doc : docs) {
                ar.add(JsonDocumentWriter.getJSON(doc, schemas));
            }

            json.element("entity-type", "documents");

            if (docs instanceof PaginableDocumentModelList) {
                PaginableDocumentModelList provider = (PaginableDocumentModelList) docs;
                json.element("isPaginable", true);
                json.element("totalSize", provider.totalSize());
                json.element("pageIndex", provider.getCurrentPageIndex());
                json.element("pageSize", provider.getPageSize());
                json.element("pageCount", provider.getNumberOfPages());
            }

            json.element("entries", ar);

            arg6.write(json.toString(2).getBytes("UTF-8"));
        } catch (Exception e) {
            log.error("Failed to wserialize document list", e);
            throw new WebApplicationException(500);
        }
    }
}

/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.model.Documents;
import org.nuxeo.ecm.automation.client.model.PaginableDocuments;

/**
 * @author matic
 *
 */
public class DocumentsMarshaller implements JsonMarshaller<Documents> {

    @Override
    public String getType() {
        return "documents";
    }

    @Override
    public Class<Documents> getJavaType() {
        return Documents.class;
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected void readDocumentEntries(JsonParser jp, Documents docs) throws Exception {
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            docs.add(DocumentMarshaller.readDocument(jp));
            tok = jp.nextToken();
        }
    }

    protected Documents readDocuments(JsonParser jp) throws Exception {
        Documents docs = new Documents();
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            String key = jp.getCurrentName();
            if ("entries".equals(key)) {
                readDocumentEntries(jp, docs);
                return docs;
            }
            tok = jp.nextToken();
        }
        return docs;
    }

    protected Documents readPaginableDocuments(JsonParser jp) throws Exception {
        PaginableDocuments docs = new PaginableDocuments();
        JsonToken tok = jp.getCurrentToken();
        while (tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("totalSize".equals(key)) {
                docs.setTotalSize(jp.getIntValue());
            } else if ("pageSize".equals(key)) {
                docs.setPageSize(jp.getIntValue());
            } else if ("pageCount".equals(key)) {
                docs.setPageCount(jp.getIntValue());
            } else if ("pageIndex".equals(key)) {
                docs.setPageIndex(jp.getIntValue());
            } else if ("entries".equals(key)) {
                readDocumentEntries(jp, docs);
            }
            tok = jp.nextToken();
        }
        return docs;
    }

    @Override
    public Documents read(JsonParser jp) throws Exception {
        jp.nextToken();
        String key = jp.getCurrentName();
        if ("isPaginable".equals(key)) {
            jp.nextToken();
            boolean isPaginable = jp.getBooleanValue();
            if (isPaginable) {
                jp.nextToken();
                return readPaginableDocuments(jp);
            }
        }
        return readDocuments(jp);
    }

}

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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;
import org.nuxeo.ecm.automation.client.jaxrs.model.PaginableDocuments;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

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
    public String getReference(Documents info) {
        return info.getInputRef();
    }
        
    @Override
    public Documents read(JSONObject json) {
        Documents docs;
        JSONArray ar = json.getJSONArray("entries");
        int size = ar.size();
        if (json.optBoolean("isPaginable") == true) {
            int totalSize = json.getInt("totalSize");
            int pageSize = json.getInt("pageSize");
            int pageCount = json.getInt("pageCount");
            int pageIndex = json.getInt("pageIndex");
            docs = new PaginableDocuments(size, totalSize, pageSize, pageCount,
                    pageIndex);
        } else {
            docs = new Documents(size);
        }
        for (int i = 0; i < size; i++) {
            JSONObject obj = ar.getJSONObject(i);
            docs.add(DocumentMarshaller.readDocument(obj));
        }
        return docs;
    }


    @Override
    public void write(JSONObject object, Documents value) {
        throw new UnsupportedOperationException();
    }
}

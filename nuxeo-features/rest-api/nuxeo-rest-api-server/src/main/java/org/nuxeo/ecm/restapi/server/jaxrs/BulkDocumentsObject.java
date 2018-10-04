/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.Map.Entry;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * Web object that allows to deal with documents list.
 *
 * @since 5.7.3
 * @deprecated since 10.3, use {@link BulkActionFrameworkObject BAF} instead
 */
@WebObject(type = "bulk")
@Deprecated
public class BulkDocumentsObject extends DefaultObject {

    protected DocumentModelList docs;

    @Override
    public <A> A getAdapter(Class<A> adapter) {
        if (adapter == DocumentModel.class) {
            return adapter.cast(docs);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        docs = (DocumentModelList) args[0];
    }

    @GET
    public DocumentModelList doGet() {
        return docs;
    }

    @DELETE
    public Response doDelete() {
        CoreSession session = getContext().getCoreSession();
        for (DocumentModel doc : docs) {
            session.removeDocument(doc.getRef());
        }
        session.save();
        return Response.ok().build();
    }

    @PUT
    public DocumentModelList doUpdate(DocumentModel updateDoc) {
        CoreSession session = getContext().getCoreSession();

        for (DocumentModel doc : docs) {
            updateDirtyFields(updateDoc, doc);
            session.saveDocument(doc);
        }
        session.save();
        return docs;
    }

    /**
     * Copy the dirty fields of srcDoc to dstDoc.
     *
     * @param srcDoc
     * @param dstDoc
     * @since 5.7.3
     */
    private void updateDirtyFields(DocumentModel srcDoc, DocumentModel dstDoc) {
        for (Entry<String, DataModel> entry : srcDoc.getDataModels().entrySet()) {
            String schemaName = entry.getKey();
            for (String field : entry.getValue().getDirtyFields()) {
                Object value = srcDoc.getDataModel(schemaName).getValue(field);
                dstDoc.getDataModel(schemaName).setValue(field, value);
            }
        }
    }

}

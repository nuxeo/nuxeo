/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

import java.util.Map;

@Operation(id = CopySchema.ID, category = Constants.CAT_DOCUMENT, label = "Copy Schema", description = "Copy all the info in the schema of the source to the input document.")
public class CopySchema {

    public static final String ID = "Document.CopySchema";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Param(name = "source", required = false)
    protected DocumentModel source;

    @Param(name = "schema", required = true)
    protected String schema;

    @OperationMethod
    public DocumentModel run(DocumentModel docToUpdate) {
        if (source == null) {
            source = (DocumentModel) context.get("request");
        }
        Map<String, Object> sourceDataModelMap = source.getProperties(schema);
        for (Map.Entry<String, Object> pair : sourceDataModelMap.entrySet()) {
            docToUpdate.setProperty(schema, pair.getKey(), pair.getValue());
        }
        return docToUpdate;
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) {
        if (source == null)
            source = (DocumentModel) context.get("request");
        DataModel model = source.getDataModel(schema);
        if (model != null) {
            for (DocumentModel doc : docs) {
                DataModel targetDM = doc.getDataModel(schema);
                if (targetDM != null) {
                    // explicitly set values so that the dirty flags are set !
                    targetDM.setMap(model.getMap());
                }
            }
        }
        return docs;
    }

}

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
 *     Miguel Nixo
 *     Ricardo Dias
 *     Thibaud Arguillere
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 8.3
 */
@Operation(id = ResetSchema.ID, category = Constants.CAT_DOCUMENT, label = "Reset Schema",
    description = "Reset all properties for a given schema or xpath. If saveDocument is true, the document is saved. If save is true, the session is saved (setting save to true and saveDocument to false has no effect, the session will not be saved).<p>WARNING: Default values are true for both saveDocument and save, which means the document is saved by default. saveDocument must be set to false when the operation is used in the context of an event that will fail if the document is saved (empty document created, about to create, before modification, ...).</p>")
public class ResetSchema {

    public static final String ID = "Document.ResetSchema";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Param(name = "schema", required = false)
    protected String schema;

    @Param(name = "xpath", required = false)
    protected String xpath;

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @Param(name = "saveDocument", required = false, values = { "true" })
    protected boolean saveDocument = true;

    private void resetSchemaProperties(DocumentModel target) throws OperationException {
        if (xpath != null) {
            target.setPropertyValue(xpath, null);
        } else if (schema != null) {
            for (String key : target.getProperties(schema).keySet()) {
                target.setProperty(schema, key, null);
            }
        } else {
            throw new OperationException("No schema or xpath was provided");
        }
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel target) throws OperationException {
        resetSchemaProperties(target);
        if (saveDocument) {
            target = session.saveDocument(target);
            if (save) {
                session.save();
            }
        }
        return target;
    }
}

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
package org.nuxeo.ecm.automation.core.operations.document;

import java.io.Serializable;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = SetDocumentProperty.ID, category = Constants.CAT_DOCUMENT, label = "Update Property", description = "Set a single property value on the input document. The property is specified using its xpath. <p>Save parameter automatically saves the document in the database. It has to be turned off when this operation is used in the context of the empty document created, about to create, before document modification, document modified events.</p> Returns the modified document.")
public class SetDocumentProperty {

    public static final String ID = "Document.SetProperty";

    @Context
    protected CoreSession session;

    @Param(name = "xpath")
    protected String xpath;

    @Param(name = "value", required = false)
    protected Serializable value;

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws OperationException {
        Property p = doc.getProperty(xpath);
        Type type = p.getField().getType();
        if (!type.isSimpleType()) {
            throw new OperationException("Only scalar types can be set using update operation");
        }
        if (value == null) {
            p.setValue(null);
        } else if (value.getClass() == String.class) {
            p.setValue(((SimpleType) type).getPrimitiveType().decode((String) value));
        } else {
            p.setValue(value);
        }
        if (save) {
            doc = session.saveDocument(doc);
        }

        return doc;
    }

}

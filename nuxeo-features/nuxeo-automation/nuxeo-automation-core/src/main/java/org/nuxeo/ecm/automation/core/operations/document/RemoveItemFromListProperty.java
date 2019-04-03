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
 *     Ricardo Dias
 */

package org.nuxeo.ecm.automation.core.operations.document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.automation.AutomationService;
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
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.schema.types.ListType;

/**
 * @since 8.3
 */
@Operation(id = RemoveItemFromListProperty.ID, category = Constants.CAT_DOCUMENT, label = "Removes an Entry From a Multivalued Property", description = "This operation removes an entry from a multivalued property, specified using a xpath (e.g.: contract:customers). A specific entry can be removed using its index number. If the index parameter is left empty, all entries in the property are removed. Activating the save parameter forces the changes to be written in database immediately (at the cost of performance loss), otherwise changes made to the document will be written in bulk when the chain succeeds. <p>Save parameter has to be turned off when this operation is used in the context of the empty document created, about to create, before document modification, document modified events.</p>")
public class RemoveItemFromListProperty {

    public static final String ID = "Document.RemoveItemFromListProperty";

    @Context
    protected CoreSession session;

    @Context
    protected AutomationService service;

    @Context
    protected OperationContext ctx;

    @Param(name = "xpath")
    protected String xpath;

    @Param(name = "index", required = false)
    protected Integer index;

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws OperationException {
        Property property = doc.getProperty(xpath);
        if (!property.isList()) {
            throw new OperationException(String.format("Property: %s of type: %s is not supported by this operation",
                    xpath, property.getType().getName()));
        }

        ListType listType = (ListType) property.getType();
        if (listType.isArray()) {
            removeItemFromArrayProperty(doc, property);
        } else {
            removeItemFromListProperty(property);
        }

        doc = session.saveDocument(doc);
        if (save) {
            session.save();
        }
        return doc;
    }

    protected void removeItemFromArrayProperty(DocumentModel doc, Property property) {
        if (index != null) {
            Serializable[] value = (Serializable[]) property.getValue();
            List<Serializable> list = new ArrayList<>(Arrays.asList(value));
            list.remove(index.intValue());
            doc.setPropertyValue(xpath, (Serializable) list);
        } else {
            doc.setPropertyValue(xpath, null);
        }
    }

    protected void removeItemFromListProperty(Property property) {
        ListProperty listProperty = (ListProperty) property;
        if (index != null) {
            listProperty.remove(index.intValue());
        } else {
            listProperty.clear();
        }
    }

}

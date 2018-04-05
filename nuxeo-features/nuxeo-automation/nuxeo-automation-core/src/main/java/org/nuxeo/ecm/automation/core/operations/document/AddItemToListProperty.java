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
 *     Frédéric Vadon
 *     Ricardo Dias
 */

package org.nuxeo.ecm.automation.core.operations.document;

import java.io.IOException;
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
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.ListType;

/**
 * @since 8.3
 */
@Operation(id = AddItemToListProperty.ID, category = Constants.CAT_DOCUMENT, label = "Adds an Entry Into a Multivalued Complex Property", description = "This operation can add new entries to a multivalued complex property. The xpath parameter is the property that should be updated (e.g.: contract:customers). The value parameter is a String containing the JSON-formatted list of entries to add. E.g.: assuming a Contract document type holding customers, each having a firstName and lastName property: [{\"lastName\":\"Norris\", \"firstName\": \"Chuck\"}, {\"lastName\":\"Lee\", \"firstName\": \"Bruce\"}] . Activating the save parameter forces the changes to be written in database immediately (at the cost of performance loss), otherwise changes made to the document will be written in bulk when the chain succeeds. <p>Save parameter has to be turned off when this operation is used in the context of the empty document created, about to create, before document modification, document modified events.</p>", aliases = {
        "Document.AddItemToListProperty" })
public class AddItemToListProperty {

    public static final String ID = "Document.AddItemToListProperty";

    @Context
    protected CoreSession session;

    @Context
    protected AutomationService service;

    @Context
    protected OperationContext ctx;

    @Param(name = "xpath")
    protected String xpath;

    @Param(name = "complexJsonProperties")
    protected String complexJsonProperties;

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws OperationException, IOException {
        Property complexProperty = doc.getProperty(xpath);
        ListType listType = (ListType) complexProperty.getField().getType();

        if (!listType.getFieldType().isComplexType()) {
            throw new OperationException("Property type " + listType.getFieldType().getClass().getName()
                    + " is not supported by this operation");
        }

        List<Object> newVals = ComplexTypeJSONDecoder.decodeList(listType, complexJsonProperties);
        for (Object newVal : newVals) {
            complexProperty.addValue(newVal);
        }

        doc = session.saveDocument(doc);
        if (save) {
            session.save();
        }
        return doc;
    }

}

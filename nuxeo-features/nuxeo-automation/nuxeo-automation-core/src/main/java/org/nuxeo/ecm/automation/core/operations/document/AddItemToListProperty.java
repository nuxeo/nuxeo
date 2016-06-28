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
 * @author fvadon
 */
@Operation(id = AddItemToListProperty.ID, category = Constants.CAT_DOCUMENT, label = "Adds a Property From a List Item", description = "This operation can add new fields to a multivalued complex metadata. The value parameter is a String containing the JSON list of new value for the metadata given in xpath", aliases = {
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

    @Param(name = "ComplexJsonProperties")
    protected String ComplexJsonProperties;

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws OperationException, IOException {
        Property complexProperty = doc.getProperty(xpath);
        ListType ltype = (ListType) complexProperty.getField().getType();

        if (!ltype.getFieldType().isComplexType()) {
            throw new OperationException("Property type is not supported by this operation");
        }

        List<Object> newVals = ComplexTypeJSONDecoder.decodeList(ltype, ComplexJsonProperties);
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

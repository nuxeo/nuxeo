/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * See operation documentation
 *
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.7
 */
@Operation(id = AddEntryToMultiValuedProperty.ID, category = Constants.CAT_DOCUMENT, label = "Add entry into multi-valued metadata", description = "Add value into the field expressed by the xpath parameter. This field must be a multivalued metadata.<p> 'checkExists' parameter enables to add value only if doesn't already exists in the field: <ul><li> if checked, the value will not be added if it exists already in the list</li><li>if not checked the value will be always added</li</ul>.<p> Remark: <b>only works for a field that stores a list of scalars (string, boolean, date, int, long) and not list of complex types.</b></p><p>Save parameter automatically saves the document in the database. It has to be turned off when this operation is used in the context of the empty document created, about to create, before document modification, document modified events.</p>", aliases = { "AddEntryToMultivaluedProperty" })
public class AddEntryToMultiValuedProperty extends AbstractOperationMultiValuedProperty {

    public static final String ID = "DocumentMultivaluedProperty.addItem";

    @Context
    protected CoreSession session;

    @Param(name = "xpath")
    protected String xpath;

    @Param(name = "value")
    protected Serializable value;

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @Param(name = "checkExists", required = false, values = { "true" })
    protected boolean checkExists = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws OperationException {
        Property p = doc.getProperty(xpath);
        Type type = p.getType();
        checkFieldType(type, value);

        List<Serializable> array = p.getValue() != null ? Arrays.asList((Serializable[]) p.getValue()) : null;

        Serializable newValue = addValueIntoList(array, value);

        p.setValue(newValue);

        if (save) {
            doc = session.saveDocument(doc);
            session.save();
        }

        return doc;
    }

    private Serializable addValueIntoList(List<Serializable> array, Object valueToAdd) {

        List<Object> list = new ArrayList<Object>();

        if (array != null) {
            list.addAll(array);
        }

        if (!list.contains(valueToAdd) || !checkExists) {
            list.add(valueToAdd);
        }

        return (Serializable) list;

    }
}

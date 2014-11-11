/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
@Operation(id = AddEntryToMultiValuedProperty.ID, category = Constants.CAT_DOCUMENT, label = "Add entry into multi-valued metadata", description = "Add value into the field expressed by the xpath parameter. This field must be a multivalued metadata.<p> 'checkExists' parameter enables to add value only if doesn't already exists in the field: <ul><li> if checked, the value will not be added if it exists already in the list</li><li>if not checked the value will be always added</li</ul>.<p> Remark: <b>only works for a field that stores a list of scalars (string, boolean, date, int, long) and not list of complex types.<b>")
public class AddEntryToMultiValuedProperty extends
        AbstractOperationMultiValuedProperty {

    public static final String ID = "AddEntryToMultivaluedProperty";

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
    public DocumentModel run(DocumentModel doc) throws Exception {
        Property p = doc.getProperty(xpath);
        Type type = p.getType();
        checkFieldType(type, value);

        List<Serializable> array = p.getValue() != null ? Arrays.asList((Serializable[]) p.getValue())
                : null;

        Serializable newValue = addValueIntoList(array, value);

        p.setValue(newValue);

        if (save) {
            doc = session.saveDocument(doc);
            session.save();
        }

        return doc;
    }

    private Serializable addValueIntoList(List<Serializable> array,
            Object valueToAdd) {

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

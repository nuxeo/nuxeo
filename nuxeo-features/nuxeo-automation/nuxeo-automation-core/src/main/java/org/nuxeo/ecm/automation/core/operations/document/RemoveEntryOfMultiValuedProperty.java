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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.7
 */
@Operation(id = RemoveEntryOfMultiValuedProperty.ID, category = Constants.CAT_DOCUMENT, label = "Remove Entry Of Multivalued Property", description = "Remove the first entry of the giving value in the multivalued xpath, does nothing if does not exist: <ul<li>if 'is Remove All' is check, all entry instance in the list.</li><li>if not will remove just the first one found</li></ul><p>Save parameter automatically saves the document in the database. It has to be turned off when this operation is used in the context of the empty document created, about to create, before document modification, document modified events.</p>", aliases = { "RemoveEntryOfMultivaluedProperty" })
public class RemoveEntryOfMultiValuedProperty extends AbstractOperationMultiValuedProperty {

    public static final String ID = "Document.RemoveEntryOfMultivaluedProperty";

    public static final Log log = LogFactory.getLog(RemoveEntryOfMultiValuedProperty.class);

    @Context
    protected CoreSession session;

    @Param(name = "xpath")
    protected String xpath;

    @Param(name = "value")
    protected Serializable value;

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @Param(name = "is Remove All", required = false, values = { "true" })
    protected boolean isRemoveAll = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws OperationException {

        Property p = doc.getProperty(xpath);
        Type type = p.getType();
        checkFieldType(type, value);

        List<Serializable> array = Arrays.asList((Serializable[]) p.getValue());

        if (array == null) {
            log.info(String.format("Value \"%s\" not found in %s, can't remove it", value, doc.getPathAsString()));
            return doc;
        }
        List<Serializable> list = new ArrayList<Serializable>(array);

        if (!list.contains(value)) {
            log.info(String.format("Value \"%s\" not found in %s, can't remove it", value, doc.getPathAsString()));
            return doc;
        }

        do {
            list.remove(value);
            p.setValue(list);
        } while (list.contains(value) && isRemoveAll);

        if (save) {
            doc = session.saveDocument(doc);
            session.save();
        }

        return doc;
    }

}

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

import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = UpdateDocument.ID, category = Constants.CAT_DOCUMENT, label = "Update Properties", description = "Set multiple properties on the input document. The properties are specified as <i>key=value</i> pairs separated by a new line. The key used for a property is the property xpath. To specify multi-line values you can use a \\ character followed by a new line. <p>Example:<pre>dc:title=The Document Title<br>dc:description=foo bar</pre>For updating a date, you will need to expose the value as ISO 8601 format, for instance : <p>Example:<pre>dc:title=The Document Title<br>dc:issued=@{org.nuxeo.ecm.core.schema.utils.DateParser.formatW3CDateTime(CurrentDate.date)}</pre><p>Returns back the updated document.<p>To update a multi-valued field with multiple values:<pre>custom:multivalued=a,b,c,d</pre><p>Save parameter automatically saves the document in the database. It has to be turned off when this operation is used in the context of the empty document created, about to create, before document modification, document modified events.</p>")
public class UpdateDocument {

    public static final String ID = "Document.Update";

    @Context
    protected CoreSession session;

    @Param(name = "properties")
    protected Properties properties;

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @Param(name = "changeToken", required = false)
    protected String changeToken = null;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws ConcurrentUpdateException, IOException {
        if (changeToken != null) {
            // Check for dirty update
            doc.putContextData(CoreSession.CHANGE_TOKEN, changeToken);
        }
        DocumentHelper.setProperties(session, doc, properties);
        if (save) {
            doc = session.saveDocument(doc); // may throw ConcurrentUpdateException if bad change token
        }
        return doc;
    }

}

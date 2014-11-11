/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * Save the input document
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = UpdateDocument.ID, category = Constants.CAT_DOCUMENT, label = "Update Properties", description = "Set multiple properties on the input document. The properties are specified as <i>key=value</i> pairs separated by a new line. The key used for a property is the property xpath. To specify multi-line values you can use a \\ charcater followed by a new line. <p>Example:<pre>dc:title=The Document Title<br>dc:description=foo bar</pre>. Returns back the updated document.")
public class UpdateDocument {

    public static final String ID = "Document.Update";

    @Context
    protected CoreSession session;

    @Param(name = "properties")
    protected Properties content;

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        DocumentHelper.setProperties(session, doc, content);
        if (save) {
            doc = session.saveDocument(doc);
        }
        return doc;
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) throws Exception {
        DocumentModelListImpl result = new DocumentModelListImpl(
                (int) docs.totalSize());
        for (DocumentModel doc : docs) {
            result.add(run(doc));
        }
        return result;
    }

    @OperationMethod
    public DocumentModelList run(DocumentRefList docs) throws Exception {
        DocumentModelListImpl result = new DocumentModelListImpl(
                (int) docs.totalSize());
        for (DocumentRef doc : docs) {
            result.add(run(session.getDocument(doc)));
        }
        return result;
    }

}

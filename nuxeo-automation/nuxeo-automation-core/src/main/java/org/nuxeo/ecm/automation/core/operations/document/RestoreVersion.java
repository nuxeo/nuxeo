/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Restores the input document to the given version.
 *
 * @since 5.7.3
 * @author Antoine Taillefer
 */
@Operation(id = RestoreVersion.ID, category = Constants.CAT_DOCUMENT, label = "Restore Version", description = "Restores the input document to the given version (path or UUID). The restored document is automatically saved if the 'save' parameter is true, else you need to save it later using the Save operation. Returns the restored document.")
public class RestoreVersion {

    public static final String ID = "Document.RestoreVersion";

    @Context
    protected CoreSession session;

    @Param(name = "version")
    protected DocumentRef version; // path or UUID

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        DocumentModel restoredDoc = session.restoreToVersion(doc.getRef(),
                version);
        if (save) {
            restoredDoc = session.saveDocument(restoredDoc);
        }
        return restoredDoc;
    }

}

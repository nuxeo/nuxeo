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

/**
 * Restores a document to the input version document.
 *
 * @since 5.7.3
 * @author Antoine Taillefer
 */
@Operation(id = RestoreVersion.ID, category = Constants.CAT_DOCUMENT, label = "Restore Version", description = "Restores a document to the input version document. If createVersion is true, a version of the live document will be created before restoring it to the input version. If checkout is true, a checkout will be processed after restoring the document, visible in the UI by the '+' symbol beside the version number. Returns the restored document.")
public class RestoreVersion {

    public static final String ID = "Document.RestoreVersion";

    @Context
    protected CoreSession session;

    @Param(name = "createVersion", required = false, values = "false")
    protected boolean createVersion = false;

    @Param(name = "checkout", required = false, values = "false")
    protected boolean checkout = false;

    @OperationMethod
    public DocumentModel run(DocumentModel version) throws Exception {
        DocumentModel liveDoc = session.getSourceDocument(version.getRef());
        return session.restoreToVersion(liveDoc.getRef(), version.getRef(),
                !createVersion, !checkout);
    }

}

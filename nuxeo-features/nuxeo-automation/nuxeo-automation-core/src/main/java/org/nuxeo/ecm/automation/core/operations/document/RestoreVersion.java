/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
    public DocumentModel run(DocumentModel version) {
        DocumentModel liveDoc = session.getSourceDocument(version.getRef());
        return session.restoreToVersion(liveDoc.getRef(), version.getRef(), !createVersion, !checkout);
    }

}

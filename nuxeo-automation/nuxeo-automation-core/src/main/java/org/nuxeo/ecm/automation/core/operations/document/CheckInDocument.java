/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * Check in the input document.
 */
@Operation(id = CheckInDocument.ID, category = Constants.CAT_DOCUMENT, label = "Check In", description = "Checks in the input document. Returns back the document.")
public class CheckInDocument {

    public static final String ID = "Document.CheckIn";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Param(name = "version", required = true, values = { "minor", "major" }, order = 0)
    protected String version;

    @Param(name = "comment", required = false, order = 1)
    protected String comment;

    @Param(name = "versionVarName", required = false, order = 2)
    protected String versionVarName;

    protected VersioningOption getVersioningOption() {
        return "major".equalsIgnoreCase(version) ? VersioningOption.MAJOR
                : VersioningOption.MINOR;
    }

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) throws Exception {
        if (session.isCheckedOut(doc)) {
            DocumentRef ver = session.checkIn(doc, getVersioningOption(),
                    comment);
            if (versionVarName != null) {
                ctx.put(versionVarName, ver);
            }
        } else {
            if (versionVarName != null) {
                ctx.put(versionVarName, session.getLastDocumentVersion(doc));
            }
        }
        return session.getDocument(doc);
    }

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        if (doc.isCheckedOut()) {
            DocumentRef ver = session.checkIn(doc.getRef(),
                    getVersioningOption(), comment);
            doc.refresh(DocumentModel.REFRESH_STATE, null);
            if (versionVarName != null) {
                ctx.put(versionVarName, ver);
            }
        } else {
            if (versionVarName != null) {
                ctx.put(versionVarName,
                        session.getLastDocumentVersion(doc.getRef()));
            }
        }
        return doc;
    }


}

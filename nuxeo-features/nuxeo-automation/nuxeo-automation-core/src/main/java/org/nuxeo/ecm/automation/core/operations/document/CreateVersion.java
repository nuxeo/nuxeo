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

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

/**
 * Save the input document
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = CreateVersion.ID, category = Constants.CAT_DOCUMENT, label = "Snapshot Version", description = "Create a new version for the input document. Any modification made on the document by the chain will be automatically saved. Increment version if this was specified through the 'snapshot' parameter. Returns the live document (not the version).")
public class CreateVersion {

    public static final String ID = "Document.CreateVersion";

    @Context
    protected CoreSession session;

    @Param(name = "increment", required = false, widget = Constants.W_OPTION, values = {
            "None", "Minor", "Major" })
    protected String snapshot = "None";

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        VersioningActions va = null;
        if ("Minor".equals(snapshot)) {
            va = VersioningActions.ACTION_INCREMENT_MINOR;
        } else if ("Major".equals(snapshot)) {
            va = VersioningActions.ACTION_INCREMENT_MAJOR;
        }
        if (va != null) {
            ScopedMap ctxData = doc.getContextData();
            ctxData.putScopedValue(ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
            ctxData.putScopedValue(ScopeType.REQUEST,
                    VersioningActions.KEY_FOR_INC_OPTION, va);
        } else {
            ScopedMap ctxData = doc.getContextData();
            ctxData.putScopedValue(ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, false);
        }
        // return session.saveDocument(doc);
        return DocumentHelper.saveDocument(session, doc);
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

}

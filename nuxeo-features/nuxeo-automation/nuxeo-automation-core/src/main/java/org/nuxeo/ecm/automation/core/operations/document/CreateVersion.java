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
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.versioning.VersioningService;

/**
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

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        VersioningOption vo;
        if ("Minor".equals(snapshot)) {
            vo = VersioningOption.MINOR;
        } else if ("Major".equals(snapshot)) {
            vo = VersioningOption.MAJOR;
        } else {
            vo = null;
        }
        if (vo != null) {
            doc.putContextData(VersioningService.VERSIONING_OPTION, vo);
        }
        // return session.saveDocument(doc);
        return DocumentHelper.saveDocument(session, doc);
    }

}

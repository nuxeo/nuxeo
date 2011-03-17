/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RemoveDocumentBlob.ID, category = Constants.CAT_BLOB, label = "Remove File", description = "Remove the file attached to the input document as specified by the 'xpath' parameter. If the 'xpath' point to a blob list then the list will be cleared. If the file to remove is part of a list it will be removed from the list otherwise the 'xpath' should point to a blob property that will be removed. If the save parameter is set the document modification will be automatically saved. Return the document.")
public class RemoveDocumentBlob {

    public static final String ID = "Blob.Remove";

    @Context
    protected CoreSession session;

    @Param(name = "xpath", required = false, values = "file:content")
    protected String xpath = "file:content";

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        DocumentHelper.removeProperty(doc, xpath);
        if (save) {
            doc = session.saveDocument(doc);
        }
        return doc;
    }

}

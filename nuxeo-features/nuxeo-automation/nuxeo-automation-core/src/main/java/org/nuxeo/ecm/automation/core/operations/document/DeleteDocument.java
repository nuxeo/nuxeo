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
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = DeleteDocument.ID, category = Constants.CAT_DOCUMENT, label = "Delete", description = "Delete the input document. The previous context input will be restored for the next operation.")
public class DeleteDocument {

    public static final String ID = "Document.Delete";

    @Context
    protected CoreSession session;

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) throws Exception {
        // if (soft) {
        // //TODO impl safe delete
        // throw new UnsupportedOperationException("Safe delete not yet
        // implemented");
        // }
        session.removeDocument(doc);
        // TODO ctx.pop
        return null;
    }

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        // if (soft) {
        // //TODO impl safe delete
        // throw new UnsupportedOperationException("Safe delete not yet
        // implemented");
        // }
        session.removeDocument(doc.getRef());
        // TODO ctx.pop
        return null;
    }


}

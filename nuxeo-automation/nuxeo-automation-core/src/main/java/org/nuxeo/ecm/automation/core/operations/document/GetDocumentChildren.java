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
import org.nuxeo.ecm.automation.core.collectors.DocumentModelListCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = GetDocumentChildren.ID, category = Constants.CAT_DOCUMENT, label = "Get Children", description = "Get the children of a document. The list of children will become the input for the next operation")
public class GetDocumentChildren {

    public static final String ID = "Document.GetChildren";

    @Context
    protected CoreSession session;

    @OperationMethod(collector=DocumentModelListCollector.class)
    public DocumentModelList run(DocumentModel doc) throws Exception {
        return session.getChildren(doc.getRef());
    }

    @OperationMethod(collector=DocumentModelListCollector.class)
    public DocumentModelList run(DocumentRef doc) throws Exception {
        return session.getChildren(doc);
    }

}

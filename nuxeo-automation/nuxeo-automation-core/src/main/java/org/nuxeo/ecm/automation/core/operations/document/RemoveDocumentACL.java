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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACP;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RemoveDocumentACL.ID, category = Constants.CAT_DOCUMENT, label = "Remove ACL", description = "Remove a named Acces Control List from the input document(s). Returns the document(s).")
public class RemoveDocumentACL {

    public static final String ID = "Document.RemoveACL";

    @Context
    protected CoreSession session;

    @Param(name = "acl")
    protected String aclName;

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        deleteACL(doc.getRef());
        return session.getDocument(doc.getRef());
    }

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) throws Exception {
        deleteACL(doc);
        return session.getDocument(doc);
    }

    protected void deleteACL(DocumentRef ref) throws ClientException {
        ACP acp = session.getACP(ref);
        acp.removeACL(aclName);
        acp.removeACL("inherited"); // make sure to not save the inherited acl
        // which is dynamically computed
        session.setACP(ref, acp, true);
    }

}

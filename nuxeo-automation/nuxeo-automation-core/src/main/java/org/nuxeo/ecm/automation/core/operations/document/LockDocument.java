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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = LockDocument.ID, category = Constants.CAT_DOCUMENT, label = "Lock", description = "Lock the input document for the current user. Returns back the locked document.")
public class LockDocument {

    public static final String ID = "Document.Lock";

    @Context
    protected CoreSession session;

    /** @deprecated unused */
    @Deprecated
    @Param(name = "owner", required = false)
    protected String owner;

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) throws Exception {
        session.setLock(doc);
        return session.getDocument(doc);
    }

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        session.setLock(doc.getRef());
        return session.getDocument(doc.getRef());
    }

}

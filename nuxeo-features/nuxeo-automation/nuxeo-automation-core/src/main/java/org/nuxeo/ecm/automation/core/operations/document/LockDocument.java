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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * Save the input document
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = LockDocument.ID, category = Constants.CAT_DOCUMENT, label = "Lock", description = "Lock the input document in the name of the given 'owner'. The lock owner is an username and identify the user that owns the lock on the document. If the owner is not specified the current user will be used as the owner. Return back the locked document")
public class LockDocument {

    public static final String ID = "Document.Lock";

    @Context
    protected CoreSession session;

    @Param(name = "owner", required = false)
    protected String value;

    @OperationMethod
    public DocumentRef run(DocumentRef doc) throws Exception {
        if (value == null) {
            value = session.getPrincipal().getName();
        }
        session.setLock(doc, value);
        return doc;
    }

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        if (value == null) {
            value = session.getPrincipal().getName();
        }
        doc.setLock(value);
        return doc;
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

    @OperationMethod
    public DocumentModelList run(DocumentRefList docs) throws Exception {
        DocumentModelListImpl result = new DocumentModelListImpl(
                (int) docs.totalSize());
        for (DocumentRef doc : docs) {
            result.add(session.getDocument(run(doc)));
        }
        return result;
    }

}

/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
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
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;

/**
 * Operation unblocking permission inheritance on a given ACL.
 *
 * @since 7.4
 */
@Operation(id = UnblockPermissionInheritance.ID, category = Constants.CAT_DOCUMENT, label = "Unlock Permission Inheritance", description = "Unlock the permission inheritance on the input document(s). Returns the document(s).")
public class UnblockPermissionInheritance {

    public static final String ID = "Document.UnblockPermissionInheritance";

    @Context
    protected CoreSession session;

    @Param(name = "acl", required = false, values = { ACL.LOCAL_ACL }, description = "ACL name.")
    String aclName = ACL.LOCAL_ACL;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        unblockPermissionInheritance(doc);
        return session.getDocument(doc.getRef());
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) {
        DocumentModel doc = session.getDocument(docRef);
        unblockPermissionInheritance(doc);
        return doc;
    }

    protected void unblockPermissionInheritance(DocumentModel doc) {
        ACP acp = doc.getACP() != null ? doc.getACP() : new ACPImpl();
        boolean permissionChanged = acp.unblockInheritance(aclName);
        if (permissionChanged) {
            doc.setACP(acp, true);
        }
    }
}

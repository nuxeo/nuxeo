/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
@Operation(id = UnblockPermissionInheritance.ID, category = Constants.CAT_DOCUMENT, label = "Unblock Permission Inheritance", description = "Unblock the permission inheritance on the input document(s). Returns the document(s).")
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

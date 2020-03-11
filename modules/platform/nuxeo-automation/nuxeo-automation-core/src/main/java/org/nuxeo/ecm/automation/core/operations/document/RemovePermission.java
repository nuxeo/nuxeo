/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
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
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;

/**
 * Operation that removes all permissions on a given ACL for a given user.
 *
 * @since 5.8
 */
@Operation(id = RemovePermission.ID, category = Constants.CAT_DOCUMENT, label = "Remove Permission", description = "Remove a permission given its id or all permissions for a given user on the input document(s). Parameter 'id' or 'user' must be set. Returns the document(s).")
public class RemovePermission {

    public static final String ID = "Document.RemovePermission";

    @Context
    protected CoreSession session;

    /**
     * @since 7.3
     */
    @Param(name = "id", required = false)
    protected String id;

    @Param(name = "user", required = false)
    protected String user;

    @Param(name = "acl", required = false)
    String aclName = ACL.LOCAL_ACL;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        removePermission(doc);
        return session.getDocument(doc.getRef());
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) {
        DocumentModel doc = session.getDocument(docRef);
        removePermission(doc);
        return doc;
    }

    protected void removePermission(DocumentModel doc) {
        if (id == null && user == null) {
            throw new IllegalParameterException("'id' or 'user' parameter must be set");
        }

        ACP acp = doc.getACP() != null ? doc.getACP() : new ACPImpl();
        boolean permissionChanged = false;
        if (user != null) {
            permissionChanged = acp.removeACEsByUsername(aclName, user);

        } else if (id != null) {
            ACE ace = ACE.fromId(id);
            permissionChanged = acp.removeACE(aclName, ace);
        }

        if (permissionChanged) {
            doc.setACP(acp, true);
        }
    }

}

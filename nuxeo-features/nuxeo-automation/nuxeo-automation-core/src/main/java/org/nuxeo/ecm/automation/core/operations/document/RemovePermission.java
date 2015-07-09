/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
            permissionChanged = acp.removeACEById(aclName, id);
        }

        if (permissionChanged) {
            doc.setACP(acp, true);
        }
    }

}

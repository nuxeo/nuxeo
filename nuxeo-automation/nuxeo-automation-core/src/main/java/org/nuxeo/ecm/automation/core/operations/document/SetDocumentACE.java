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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;

/**
 * Save the input document
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = SetDocumentACE.ID, category = Constants.CAT_DOCUMENT, label = "Set ACL", description = "Set Acces Control Entry on the input document(s). Returns the document(s).")
public class SetDocumentACE {

    public static final String ID = "Document.SetACE";

    @Context
    protected CoreSession session;

    @Param(name = "user")
    protected String user;

    @Param(name = "permission")
    String permission;

    @Param(name = "acl", required = false, values = ACL.LOCAL_ACL)
    String aclName = ACL.LOCAL_ACL;

    @Param(name = "grant", required = false, values = "true")
    boolean grant = true;

    @Param(name = "overwrite", required = false, values = "true")
    boolean overwrite = true;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        setACE(doc.getRef());
        return session.getDocument(doc.getRef());
    }

    @OperationMethod
    public DocumentRef run(DocumentRef doc) throws Exception {
        setACE(doc);
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

    protected void setACE(DocumentRef ref) throws ClientException {
        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl(aclName);
        acp.addACL(acl);
        ACE ace = new ACE(user, permission, grant);
        acl.add(ace);
        session.setACP(ref, acp, false);
    }

}

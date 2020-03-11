/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = SetDocumentACE.ID, category = Constants.CAT_DOCUMENT, label = "Set ACL", description = "Set Acces Control Entry on the input document(s). Returns the document(s).", aliases = { "Document.SetACE" })
public class SetDocumentACE {

    public static final String ID = "Document.AddACE";

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

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        setACE(doc.getRef());
        return session.getDocument(doc.getRef());
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) {
        setACE(doc);
        return session.getDocument(doc);
    }

    protected void setACE(DocumentRef ref) {
        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl(aclName);
        acp.addACL(acl);
        ACE ace = new ACE(user, permission, grant);
        acl.add(ace);
        session.setACP(ref, acp, overwrite);
    }

}

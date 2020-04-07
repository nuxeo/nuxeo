/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.repository;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;

/**
 * Root creator for persistence.
 *
 * @since 11.1
 */
public class UnrestrictedRootCreator extends UnrestrictedSessionRunner {

    protected DocumentRef rootRef;

    protected final String parentPath;

    protected final String name;

    protected final boolean setAcl;

    public UnrestrictedRootCreator(CoreSession session, String parentPath, String name, boolean setAcl) {
        super(session);
        this.name = name;
        this.parentPath = parentPath;
        this.setAcl = setAcl;
    }

    public DocumentRef getRootRef() {
        return rootRef;
    }

    @Override
    public void run() {

        DocumentModel root = session.createDocumentModel(parentPath, name, "Workspace");
        root.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, name);
        root = session.createDocument(root);

        if (setAcl) {
            ACL acl = new ACLImpl();
            acl.add(new ACE(SnapshotPersister.Write_Grp, "Write", true));
            acl.add(new ACE(SnapshotPersister.Read_Grp, "Read", true));
            ACP acp = root.getACP();
            acp.addACL(acl);
            session.setACP(root.getRef(), acp, true);
        }

        rootRef = root.getRef();
        // flush caches
        session.save();
    }

}

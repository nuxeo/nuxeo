/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.publisher.task;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACP;

/**
 * @author arussel
 * @author ataillefer XXX ataillefer: get rid of oldAclName if refactor old JBPM ACL name
 */
public class RemoveACLUnrestricted extends UnrestrictedSessionRunner {
    private final DocumentModel document;

    private final String aclName;

    private final String oldAclName;

    public RemoveACLUnrestricted(CoreSession session, DocumentModel document, String aclName, String oldAclName) {
        super(session);
        this.document = document;
        this.aclName = aclName;
        this.oldAclName = oldAclName;
    }

    @Override
    public void run() {
        ACP acp = document.getACP();
        acp.removeACL(aclName);
        acp.removeACL(oldAclName);
        session.setACP(document.getRef(), acp, true);
        session.save();
        document.getACP(); // load ACP
    }

}

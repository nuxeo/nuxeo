/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.publisher.task;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Changes the permission on a document to only allow validators.
 */
public class ChangePermissionUnrestricted extends UnrestrictedSessionRunner {

    private final DocumentRef ref;

    private final NuxeoPrincipal principal;

    private final String aclName;

    private final String[] validators;

    // acl unused
    public ChangePermissionUnrestricted(CoreSession session, DocumentModel document, String[] validators,
            NuxeoPrincipal principal, String aclName, ACL acl) {
        super(session);
        this.ref = document.getRef();
        this.validators = validators;
        this.principal = principal;
        this.aclName = aclName;
    }

    @Override
    public void run() {
        ACP acp = session.getACP(ref);
        ACL acl = acp.getOrCreateACL(aclName);
        acl.clear();
        for (String validator : validators) {
            acl.add(new ACE(validator, SecurityConstants.READ));
            acl.add(new ACE(validator, SecurityConstants.WRITE));
        }
        // Give View permission to the user who submitted for publishing.
        acl.add(new ACE(principal.getName(), SecurityConstants.READ));
        // Allow administrators too.
        UserManager userManager = Framework.getService(UserManager.class);
        for (String group : userManager.getAdministratorsGroups()) {
            acl.add(new ACE(group, SecurityConstants.EVERYTHING));
        }
        // Deny everyone else.
        acl.add(ACE.BLOCK);
        session.setACP(ref, acp, true);
        session.save();
    }

}

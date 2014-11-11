/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.publisher.task;

import org.nuxeo.ecm.core.api.ClientException;
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
    public ChangePermissionUnrestricted(CoreSession session,
            DocumentModel document, String[] validators,
            NuxeoPrincipal principal, String aclName, ACL acl) {
        super(session);
        this.ref = document.getRef();
        this.validators = validators;
        this.principal = principal;
        this.aclName = aclName;
    }

    @Override
    public void run() throws ClientException {
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

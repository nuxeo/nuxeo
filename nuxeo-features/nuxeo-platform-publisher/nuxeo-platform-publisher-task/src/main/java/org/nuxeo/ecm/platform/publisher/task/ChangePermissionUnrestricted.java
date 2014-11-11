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
 *     arussel
 */
package org.nuxeo.ecm.platform.publisher.task;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author arussel
 *
 */
public class ChangePermissionUnrestricted extends UnrestrictedSessionRunner {
    private final DocumentModel document;

    private final NuxeoPrincipal principal;

    private final String aclName;

    private final String[] validators;

    private final ACL existingACL;

    public ChangePermissionUnrestricted(CoreSession session,
            DocumentModel document, String[] validators,
            NuxeoPrincipal principal, String aclName, ACL acl) {
        super(session);
        this.document = document;
        this.validators = validators;
        this.principal = principal;
        this.aclName = aclName;
        this.existingACL = acl;
    }

    @Override
    public void run() throws ClientException {
        List<UserEntry> userEntries = new ArrayList<UserEntry>();
        for (String validator : validators) {
            UserEntry ue = new UserEntryImpl(validator);
            ue.addPrivilege(SecurityConstants.READ, true, false);
            userEntries.add(ue);
            ue = new UserEntryImpl(validator);
            ue.addPrivilege(SecurityConstants.WRITE, true, false);
            userEntries.add(ue);
        }

        // Give View permission to the user who submitted for publishing.
        UserEntry ue = new UserEntryImpl(principal.getName());
        ue.addPrivilege(SecurityConstants.READ, true, false);
        userEntries.add(ue);

        // Deny everyone the write and read access once process has started.
        UserEntry everyoneElse = new UserEntryImpl(SecurityConstants.EVERYONE);
        everyoneElse.addPrivilege(SecurityConstants.WRITE, false, false);
        userEntries.add(everyoneElse);
        UserEntry everyoneView = new UserEntryImpl(SecurityConstants.EVERYONE);
        everyoneView.addPrivilege(SecurityConstants.READ, false, false);
        userEntries.add(everyoneView);
        ACP acp = document.getACP();
        acp.setRules(aclName, userEntries.toArray(new UserEntry[] {}));
        session.setACP(document.getRef(), acp, true);
        session.save();
    }

}

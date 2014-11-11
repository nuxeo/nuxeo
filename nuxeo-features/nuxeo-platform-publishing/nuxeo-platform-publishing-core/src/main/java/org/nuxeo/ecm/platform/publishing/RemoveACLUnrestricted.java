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
package org.nuxeo.ecm.platform.publishing;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACP;

/**
 * @author arussel
 */
public class RemoveACLUnrestricted extends UnrestrictedSessionRunner {
    private final DocumentModel document;
    private final String aclName;
    public RemoveACLUnrestricted(CoreSession session, DocumentModel document, String aclName) {
        super(session);
        this.document = document;
        this.aclName = aclName;
    }

    @Override
    public void run() throws ClientException {
        ACP acp = document.getACP();
        acp.removeACL(aclName);
        session.setACP(document.getRef(), acp, true);
        session.save();
    }

}

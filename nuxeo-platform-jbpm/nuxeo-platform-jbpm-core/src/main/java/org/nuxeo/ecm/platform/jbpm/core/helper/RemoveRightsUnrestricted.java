/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     alexandre
 */
package org.nuxeo.ecm.platform.jbpm.core.helper;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACP;

/**
 * @author alexandre
 *
 */
public class RemoveRightsUnrestricted extends UnrestrictedSessionRunner {

    private final DocumentRef docRef;

    private final String aclName;

    public RemoveRightsUnrestricted(CoreSession session, DocumentRef docRef,
            String aclName) {
        super(session);
        this.docRef = docRef;
        this.aclName = aclName;
    }

    @Override
    public void run() throws ClientException {
        ACP acp = session.getACP(docRef);
        acp.removeACL(aclName);
        session.setACP(docRef, acp, true);
        session.save();
    }

}

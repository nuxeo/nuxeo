/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.jbpm.core.helper;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;

/**
 * Unrestricted process abandon.
 *
 * @since 5.6
 */
public class AbandonProcessUnrestricted extends UnrestrictedSessionRunner {

    private final DocumentRef ref;

    private final Long processId;

    public AbandonProcessUnrestricted(CoreSession session, DocumentRef ref,
            Long processId) {
        super(session);
        this.ref = ref;
        this.processId = processId;
    }

    @Override
    public void run() throws ClientException {
        DocumentModel doc = session.getDocument(ref);
        ACP acp = doc.getACP();
        acp.removeACL(AbstractJbpmHandlerHelper.getProcessACLName(processId));
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

}

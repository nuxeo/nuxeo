/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.api;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 *
 *
 */
public class UnrestrictedPropertySetter extends UnrestrictedSessionRunner {

    public static final String NEW_TITLE = "new title";

    public static final String DC_TITLE = "dc:title";

    private DocumentRef ref;
    /**
     * @param session
     */
    public UnrestrictedPropertySetter(CoreSession session) {
        super(session);
    }

    @Override
    public void run() throws ClientException {
        DocumentRef rootRef = session.getRootDocument().getRef();
        ACP acp = session.getACP(rootRef);
        ACL acl = acp.getOrCreateACL("LOCAL");
        acl.add(new ACE("bob", SecurityConstants.READ_WRITE, true));
        session.setACP(rootRef, acp, true);
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue(UnrestrictedPropertySetter.DC_TITLE,
                UnrestrictedPropertySetter.NEW_TITLE);
        doc = session.createDocument(doc);
        doc.setPropertyValue(DC_TITLE, NEW_TITLE);
        ref = doc.getRef();
        //session.save(); necessary if not in a transaction
    }

    public DocumentRef getDocRef() {
        return ref;
    }

}

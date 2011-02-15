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
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class TestUnrestrictedSessionRunner extends SQLRepositoryTestCase {

    public static final String DC_TITLE = "dc:title";

    public static final String NEW_TITLE = "new title";

    public void testUnrestrictedPropertySetter() throws Exception {
        run(openSessionAs("bob"));
    }

    /**
     * Actual test. Also run in JCA mode.
     */
    public static void run(CoreSession session) throws Exception {
        UnrestrictedPropertySetter setter = new UnrestrictedPropertySetter(
                session);
        setter.runUnrestricted();
        DocumentModel doc = session.getDocument(setter.docRef);
        assertEquals(doc.getPropertyValue(DC_TITLE), NEW_TITLE);
    }

    protected static class UnrestrictedPropertySetter extends
            UnrestrictedSessionRunner {

        public DocumentRef docRef;

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
            doc.setPropertyValue(DC_TITLE, NEW_TITLE);
            doc = session.createDocument(doc);
            docRef = doc.getRef();
        }
    }

}

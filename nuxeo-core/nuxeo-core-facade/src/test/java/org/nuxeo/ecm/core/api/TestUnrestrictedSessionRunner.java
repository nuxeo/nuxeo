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

/**
 * Test unrestricted session runner to check iso functionnality with and without
 * JCA
 *
 * @see TestUnrestrictedSessionRunnerJCA
 */
public class TestUnrestrictedSessionRunner extends SQLRepositoryTestCase {

    private static final String DOC_NAME = "doc";

    public static final String DC_TITLE = "dc:title";

    public static final String NEW_TITLE = "new title";

    public void testUnrestrictedPropertySetter() throws Exception {
        seeDocCreatedByUnrestricted(openSessionAs("bob"));
    }

    public void testUnrestrictedSessionSeesDocCreatedBefore() throws Exception {
        unrestrictedSeesDocCreatedBefore(openSessionAs("Administrator"));
    }

    /*
     * ----- Actual tests. Also run in JCA mode. -----
     */

    /**
     * An unrestricted session creates a doc then the calling session tries to
     * access it.
     */
    public static void seeDocCreatedByUnrestricted(CoreSession session)
            throws Exception {
        UnrestrictedPropertySetter setter = new UnrestrictedPropertySetter(
                session);
        setter.runUnrestricted();
        DocumentModel doc = session.getDocument(setter.docRef);
        assertEquals(doc.getPropertyValue(DC_TITLE), NEW_TITLE);
    }

    /**
     * A session creates a doc, then calls an unrestricted session that modifies
     * it.
     */
    public static void unrestrictedSeesDocCreatedBefore(CoreSession session)
            throws Exception {
        DocumentModel doc = session.createDocumentModel("/", DOC_NAME, "File");
        doc = session.createDocument(doc);
        UnrestrictedDocumentReader reader = new UnrestrictedDocumentReader(
                session, doc.getRef());
        reader.runUnrestricted();
        assertEquals(DOC_NAME, reader.getName());
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
            DocumentModel doc = session.createDocumentModel("/", DOC_NAME,
                    "File");
            doc.setPropertyValue(DC_TITLE, NEW_TITLE);
            doc = session.createDocument(doc);
            docRef = doc.getRef();
        }
    }

    protected static class UnrestrictedDocumentReader extends
            UnrestrictedSessionRunner {
        private DocumentRef ref;

        private String name;

        public UnrestrictedDocumentReader(CoreSession session, DocumentRef ref) {
            super(session);
            this.ref = ref;
        }

        @Override
        public void run() throws ClientException {
            DocumentModel doc = session.getDocument(ref);
            name = doc.getName();
        }

        public String getName() {
            return name;
        }
    }

}

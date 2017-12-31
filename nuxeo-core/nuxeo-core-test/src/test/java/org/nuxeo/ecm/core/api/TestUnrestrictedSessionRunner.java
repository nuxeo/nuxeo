/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.api;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test unrestricted session runner.
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestUnrestrictedSessionRunner {

    private static final String DOC_NAME = "doc";

    public static final String DC_TITLE = "dc:title";

    public static final String NEW_TITLE = "new title";

    @Inject
    protected CoreFeature coreFeature;

    @Test
    public void testUnrestrictedPropertySetter() throws Exception {
        try (CloseableCoreSession session = coreFeature.openCoreSession("bob")) {
            seeDocCreatedByUnrestricted(session);
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testUnrestrictedSessionSeesDocCreatedBefore() throws Exception {
        try (CloseableCoreSession session = coreFeature.openCoreSession(SecurityConstants.ADMINISTRATOR)) {
            unrestrictedSeesDocCreatedBefore(session);
        }
    }

    /*
     * ----- Actual tests. Also run in JCA mode. -----
     */

    /**
     * An unrestricted session creates a doc then the calling session tries to access it.
     */
    public static void seeDocCreatedByUnrestricted(CoreSession session) throws Exception {
        UnrestrictedPropertySetter setter = new UnrestrictedPropertySetter(session);
        setter.runUnrestricted();
        DocumentModel doc = session.getDocument(setter.docRef);
        assertEquals(doc.getPropertyValue(DC_TITLE), NEW_TITLE);
    }

    /**
     * A session creates a doc, then calls an unrestricted session that modifies it.
     */
    public static void unrestrictedSeesDocCreatedBefore(CoreSession session) throws Exception {
        DocumentModel doc = session.createDocumentModel("/", DOC_NAME, "File");
        doc = session.createDocument(doc);
        UnrestrictedDocumentReader reader = new UnrestrictedDocumentReader(session, doc.getRef());
        reader.runUnrestricted();
        assertEquals(DOC_NAME, reader.getName());
    }

    protected static class UnrestrictedPropertySetter extends UnrestrictedSessionRunner {

        public DocumentRef docRef;

        public UnrestrictedPropertySetter(CoreSession session) {
            super(session);
        }

        @Override
        public void run() {
            DocumentRef rootRef = session.getRootDocument().getRef();
            ACP acp = session.getACP(rootRef);
            ACL acl = acp.getOrCreateACL("LOCAL");
            acl.add(new ACE("bob", SecurityConstants.READ_WRITE, true));
            session.setACP(rootRef, acp, true);
            DocumentModel doc = session.createDocumentModel("/", DOC_NAME, "File");
            doc.setPropertyValue(DC_TITLE, NEW_TITLE);
            doc = session.createDocument(doc);
            docRef = doc.getRef();
        }
    }

    protected static class UnrestrictedDocumentReader extends UnrestrictedSessionRunner {
        private DocumentRef ref;

        private String name;

        public UnrestrictedDocumentReader(CoreSession session, DocumentRef ref) {
            super(session);
            this.ref = ref;
        }

        @Override
        public void run() {
            DocumentModel doc = session.getDocument(ref);
            name = doc.getName();
        }

        public String getName() {
            return name;
        }
    }

}

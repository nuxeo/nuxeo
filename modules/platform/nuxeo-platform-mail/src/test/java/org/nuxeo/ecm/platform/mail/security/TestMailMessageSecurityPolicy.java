/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *      André Justo
 */
package org.nuxeo.ecm.platform.mail.security;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MAIL_MESSAGE_TYPE;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@Deploy("org.nuxeo.ecm.platform.mail:OSGI-INF/nxmail-core-types-contrib.xml")
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestMailMessageSecurityPolicy {

    @Inject
    protected CoreSession session;

    @Test
    public void testWithoutPolicy() {
        doTest(false);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.mail:OSGI-INF/security-policy-contrib.xml")
    public void testWithPolicy() {
        doTest(true);
    }

    protected void doTest(boolean expectPermissionDenied) {
        // create a new document and give Write permissions
        DocumentModel doc = session.createDocumentModel("/", "testMailMessage", MAIL_MESSAGE_TYPE);
        doc = session.createDocument(doc);
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, new ACE("user1", SecurityConstants.READ_WRITE, true));
        doc.setACP(acp, true);
        session.save();
        CoreSession userSession = CoreInstance.getCoreSession(session.getRepositoryName(), "user1");
        if (expectPermissionDenied) {
            assertFalse(userSession.hasPermission(doc.getRef(), WRITE_PROPERTIES));
            assertFalse(userSession.hasPermission(doc.getRef(), WRITE));
        } else {
            assertTrue(userSession.hasPermission(doc.getRef(), WRITE_PROPERTIES));
            assertTrue(userSession.hasPermission(doc.getRef(), WRITE));
        }
    }
}

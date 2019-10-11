/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */
package org.nuxeo.retention.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.security.auth.login.LoginException;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 11.1
 */
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-security.xml")
public class TestRetentionSecurity extends RetentionTestCase {

    @Test
    public void shouldBeAuthorizedToManageLegalHold() throws LoginException {
        try (CloseableCoreSession userSession = CoreInstance.openCoreSession(session.getRepositoryName(), "user")) {
            service.attachRule(file, createManualImmediateRuleMillis(100L), userSession);
            fail("Sould not be abe to attach rule");
        } catch (NuxeoException e) {
            // Expected
            assertEquals(javax.servlet.http.HttpServletResponse.SC_FORBIDDEN, e.getStatusCode());
            assertFalse(session.isRecord(file.getRef()));
            assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
            assertFalse(session.getDocument(file.getRef()).hasFacet(RetentionConstants.RECORD_FACET));
        }
    }

    @Test
    public void shouldNotBeAuthorizedToAttachRule() throws LoginException {
        try (CloseableCoreSession userSession = CoreInstance.openCoreSession(session.getRepositoryName(), "user")) {
            service.attachRule(file, createManualImmediateRuleMillis(100L), userSession);
            fail("Sould not be abe to attach rule");
        } catch (NuxeoException e) {
            // Expected
            assertEquals(javax.servlet.http.HttpServletResponse.SC_FORBIDDEN, e.getStatusCode());
            assertFalse(session.isRecord(file.getRef()));
            assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
            assertFalse(session.getDocument(file.getRef()).hasFacet(RetentionConstants.RECORD_FACET));
        }
    }


    @Test
    public void shouldBeAuthorizedToAttachRule() throws LoginException {
        ACP acp = new ACPImpl();
        ACE allowAttachRule = new ACE("user", RetentionConstants.MANAGE_RECORD_PERMISSION, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { allowAttachRule });
        acp.addACL(acl);
        file.setACP(acp, true);
        file = session.saveDocument(file);
        try (CloseableCoreSession userSession = CoreInstance.openCoreSession(session.getRepositoryName(), "user")) {
            file = service.attachRule(file, createManualImmediateRuleMillis(5000L), userSession);
            assertTrue(userSession.isUnderRetentionOrLegalHold(file.getRef()));
        }
    }

    @Test
    public void shouldBeAuthorizedToSetLegalHold() throws LoginException {
        ACP acp = new ACPImpl();
        ACE allowLegalHold = new ACE("user", RetentionConstants.MANAGE_LEGAL_HOLD_PERMISSION, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { allowLegalHold });
        acp.addACL(acl);
        file.setACP(acp, true);
        file = session.saveDocument(file);
        try (CloseableCoreSession userSession = CoreInstance.openCoreSession(session.getRepositoryName(), "user")) {
            userSession.makeRecord(file.getRef());
            userSession.setLegalHold(file.getRef(), true, null);
        }
    }

    @Test
    public void shouldNotBeAllowedToAttachTwoRules() {
        RetentionRule rr = createManualImmediateRuleMillis(100L);
        file = service.attachRule(file, rr, session);
        try  {
            service.attachRule(file, rr, session);
            fail("Should not be abe to attach rule twice");
        } catch (NuxeoException e) {
            // Expected
        }
    }

}

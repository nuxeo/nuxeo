/*
 * (C) Copyright 2013-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mariana Cedica <mcedica@nuxeo.com>
 */
package org.nuxeo.ecm.quota.count;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.userworkspace.api")
@Deploy("org.nuxeo.ecm.platform.userworkspace.core")
@Deploy("org.nuxeo.ecm.platform.userworkspace.types")
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.quota")
public class TestQuotaService {

    @Inject
    protected QuotaStatsService quotaStatsService;

    @Inject
    protected UserWorkspaceService uwm;

    @Inject
    protected CoreSession session;

    @Inject
    protected CoreFeature coreFeature;

    @Test
    public void testSetQuotaOnUserWorkspaces() {
        DocumentRef uwRef1 = createUserWorkspace("jdoe");
        DocumentRef uwRef2 = createUserWorkspace("jack");

        quotaStatsService.launchSetMaxQuotaOnUserWorkspaces(100L, session.getRootDocument(), session);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        DocumentModel uw1 = session.getDocument(uwRef1);
        DocumentModel uw2 = session.getDocument(uwRef2);
        assertEquals(uw1.getProperty("dss:maxSize").getValue(Long.class), Long.valueOf(100L));
        assertEquals(uw2.getProperty("dss:maxSize").getValue(Long.class), Long.valueOf(100L));
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected DocumentRef createUserWorkspace(String username) {
        CoreSession userSession = coreFeature.getCoreSession(username);
        DocumentModel userWS = uwm.getCurrentUserPersonalWorkspace(userSession);
        assertNotNull(userWS);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        // check creator
        String creator = (String) userWS.getProperty("dublincore", "creator");
        assertEquals(username, creator);

        return userWS.getRef();
    }

}

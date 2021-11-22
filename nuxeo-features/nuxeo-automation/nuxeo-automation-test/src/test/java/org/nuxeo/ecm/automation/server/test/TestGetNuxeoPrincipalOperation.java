/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.core.operations.users.GetNuxeoPrincipal;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.10-HF55
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, EmbeddedAutomationServerFeature.class })
public class TestGetNuxeoPrincipalOperation {

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected Session session;

    @Test
    public void testOperation() throws Exception {
        Document user = (Document) session.newRequest(GetNuxeoPrincipal.ID)
                                          .set("login", coreSession.getPrincipal().getName())
                                          .execute();
        assertNotNull(user);
        assertEquals("Administrator", user.getProperties().get("dc:title"));
    }
}

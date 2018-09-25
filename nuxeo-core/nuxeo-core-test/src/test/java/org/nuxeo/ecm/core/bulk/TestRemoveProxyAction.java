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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.bulk.DocumentSetRepositoryInit.created_non_proxy;
import static org.nuxeo.ecm.core.bulk.DocumentSetRepositoryInit.created_proxy;
import static org.nuxeo.ecm.core.bulk.DocumentSetRepositoryInit.created_total;
import static org.nuxeo.ecm.core.bulk.actions.RemoveProxyAction.ACTION_NAME;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@RepositoryConfig(init = DocumentSetRepositoryInit.class)
public class TestRemoveProxyAction {

    @Inject
    public BulkService service;

    @Inject
    public CoreSession session;

    @Test
    public void testRemoveProxy() throws Exception {

        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * from Document where ecm:ancestorId='%s'", model.getId());

        assertEquals(created_proxy, session.query(nxql + " and ecm:isProxy=1").size());
        assertEquals(created_non_proxy, session.query(nxql + " and ecm:isProxy=0").size());

        String commandId = service.submit(new BulkCommand().withRepository(session.getRepositoryName())
                                                           .withUsername(session.getPrincipal().getName())
                                                           .withQuery(nxql)
                                                           .withAction(ACTION_NAME));

        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(10)));

        BulkStatus status = service.getStatus(commandId);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());
        assertEquals(created_total, status.getProcessed());

        assertEquals(0, session.query(nxql + " and ecm:isProxy=1").size());
        assertEquals(created_non_proxy, session.query(nxql + " and ecm:isProxy=0").size());

    }
}

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
import static org.junit.Assert.assertTrue;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreBulkFeature.class, CoreFeature.class })
public class TestBulkProcessor {

    @Inject
    public BulkService service;

    @Inject
    public CoreSession session;

    @Test
    public void testEmptyQuery() throws InterruptedException {
        String nxql = "SELECT * from Document where ecm:parentId='tutu'";
        assertEquals(0, session.query(nxql).size());
        service.submit(new BulkCommand().withRepository(session.getRepositoryName())
                                        .withUsername(session.getPrincipal().getName())
                                        .withQuery(nxql)
                                        .withAction("setProperties"));
        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(10)));
    }
}

/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     anechaev
 */
package org.nuxeo.ecm.platform.importer.queue.tests;

import com.google.inject.Inject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.kafka.service.DefaultKafkaService;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.manager.KQManager;
import org.nuxeo.ecm.platform.importer.queue.tests.features.KafkaFeature;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import java.io.IOException;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

@RunWith(FeaturesRunner.class)
@Features({CoreFeature.class, KafkaFeature.class})
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({
        "org.nuxeo.ecm.platform.importer.core", //
})
@LocalDeploy("org.nuxeo.ecm.platform.importer.queue.test:test-kafka-service-contrib.xml")
public class TestKQManager {

    protected static final Log log = LogFactory.getLog(TestKQManager.class);

    @Inject
    private DefaultKafkaService service;

    @Test
    public void testManager() throws IOException, InterruptedException {
        assertNotNull(service);

        ImporterLogger logger = new BufferredLogger(log);
        KQManager<SourceNode> kq = new KQManager<>(logger, 5);
        SourceNode node = new BuggySourceNode(1, false, false);
        kq.put(1, node);
        kq.put(1, node);

        SourceNode node1 = kq.poll(1);
        log.info(node1.getName());
        assertEquals(node.getName(), node1.getName());
    }
}

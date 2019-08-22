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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test of the cluster service (loading the contribution).
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.cluster")
public class TestClusterService {

    @Inject
    protected ClusterService clusterService;

    @Test
    public void testNothing() {
        assertFalse(clusterService.isEnabled());
        assertNotNull(clusterService.getNodeId());
    }

    @Test
    @Deploy("org.nuxeo.runtime.cluster.tests:OSGI-INF/test1.xml")
    public void testClusterNode() {
        assertTrue(clusterService.isEnabled());
        assertEquals("123", clusterService.getNodeId());
    }

    @Test
    @Deploy("org.nuxeo.runtime.cluster.tests:OSGI-INF/test1.xml")
    @Deploy("org.nuxeo.runtime.cluster.tests:OSGI-INF/test-disable.xml")
    public void testDisable() {
        assertFalse(clusterService.isEnabled());
        assertNotNull(clusterService.getNodeId());
    }

    @Test
    @Deploy("org.nuxeo.runtime.cluster.tests:OSGI-INF/test-blank.xml")
    public void testClusterNodeEnabledBlank() {
        assertFalse(clusterService.isEnabled());
    }

}

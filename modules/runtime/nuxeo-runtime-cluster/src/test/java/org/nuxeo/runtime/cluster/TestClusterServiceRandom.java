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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.TestClusterServiceRandom.OldClusterPropFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Test of the compatibility framework properties to define the cluster node id randomly when no configuration is
 * present but clustering is still enabled.
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ ClusterFeature.class, OldClusterPropFeature.class })
public class TestClusterServiceRandom {

    public static class OldClusterPropFeature implements RunnerFeature {

        @Override
        public void start(FeaturesRunner runner) throws Exception {
            Framework.getProperties().put("repository.clustering.enabled", "true");
            // but don't define repository.clustering.id
        }
    }

    @Inject
    protected ClusterService clusterService;

    @Test
    public void testClusterNode() {
        assertTrue(clusterService.isEnabled());
        String nodeId = clusterService.getNodeId();
        assertNotNull(nodeId);
        assertFalse(nodeId, nodeId.startsWith("-"));
    }

}

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
import static org.junit.Assert.assertTrue;
import static org.nuxeo.runtime.cluster.ClusterServiceImpl.CLUSTERING_ENABLED_OLD_PROP;
import static org.nuxeo.runtime.cluster.ClusterServiceImpl.NODE_ID_OLD_PROP;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * Test of the compatibility framework properties to define the cluster node id.
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(ClusterFeature.class)
@WithFrameworkProperty(name = CLUSTERING_ENABLED_OLD_PROP, value = "true")
@WithFrameworkProperty(name = NODE_ID_OLD_PROP, value = "1111")
public class TestClusterServiceCompat {

    @Inject
    protected ClusterService clusterService;

    @Test
    public void testClusterNode() {
        assertTrue(clusterService.isEnabled());
        assertEquals("1111", clusterService.getNodeId());
    }

}

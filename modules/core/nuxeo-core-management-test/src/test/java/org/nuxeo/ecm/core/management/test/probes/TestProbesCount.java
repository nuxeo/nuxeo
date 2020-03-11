/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.management.test.probes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.probes.ProbeManagerImpl;
import org.nuxeo.ecm.core.management.statuses.HealthCheckResult;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.runtime.management")
@Deploy("org.nuxeo.ecm.core.management")
@Deploy("org.nuxeo.ecm.core.management.test")
public class TestProbesCount {

    @Inject
    CoreSession session;

    @Inject
    ProbeManager pm;

    @AfterClass
    public static void cleanupProbes() {
        // set healthCheck interval back to default value after test
        Framework.getProperties().remove(ProbeManagerImpl.DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS_PROPERTY);
    }

    @Test
    public void testConsecutiveCallsOnHealthCheck() {
        HealthCheckResult result = pm.getOrRunHealthCheck("runtimeStatus");
        ProbeInfo probeInfo = pm.getProbeInfo("runtimeStatus");

        assertTrue(result.isHealthy());
        assertTrue(probeInfo.getStatus().isSuccess());
        // run again and test that the probe was only invoked once since the default check interval is 20s
        result = pm.getOrRunHealthCheck("runtimeStatus");
        probeInfo = pm.getProbeInfo("runtimeStatus");
        assertEquals(1, probeInfo.getRunnedCount());

        // modify the default check interval to -1, run again and test that the probe was invoked
        Framework.getProperties().setProperty(ProbeManagerImpl.DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS_PROPERTY, "-1");

        result = pm.getOrRunHealthCheck("runtimeStatus");
        probeInfo = pm.getProbeInfo("runtimeStatus");
        assertEquals(2, probeInfo.getRunnedCount());

    }

}

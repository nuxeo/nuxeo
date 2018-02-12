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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.probes.AdministrativeStatusProbe;
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
public class TestProbes {

    @Inject
    CoreSession session;

    @Inject
    ProbeManager pm;

    @Inject
    FakeService fs;

    protected static final int TEST_INTERVAL_SECONDS = -1;

    @Before
    public void removeCacheOnProbes() {
        // remove effects linked to cache for these tests
        Framework.getProperties().setProperty(ProbeManagerImpl.DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS_PROPERTY,
                String.valueOf(TEST_INTERVAL_SECONDS));
        // reset fake service status
        fs.setSuccess();
    }

    @After
    public void cleanupProbes() {
        // set healthCheck interval back to default value after each test
        Framework.getProperties().remove(ProbeManagerImpl.DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS_PROPERTY);
    }

    @Test
    public void testServiceLookup() {
        assertNotNull(pm);
    }

    @Test
    public void testService() {

        pm.runAllProbes();

        ProbeInfo info = pm.getProbeInfo(AdministrativeStatusProbe.class);
        assertNotNull(info);

        info = pm.getProbeInfo("administrativeStatus");
        assertNotNull(info);

        Collection<String> names = pm.getProbeNames();
        assertTrue("admin status shortcut not listed", names.contains("administrativeStatus"));
        assertNotNull("admin status probe not published", info.getQualifiedName());

        assertEquals(1, info.getRunnedCount());
        assertFalse("not a success", info.isInError());
        assertFalse("wrong success value", info.getStatus().getAsString().equals("[unavailable]"));
        assertEquals("wrong default value", "[unavailable]", info.getLastFailureStatus().getAsString());

    }

    @Test
    public void testHealthCheck() throws IOException {
        Collection<ProbeInfo> healthCheckProbes = pm.getHealthCheckProbes();
        assertEquals(3, healthCheckProbes.size());
        HealthCheckResult result = pm.getOrRunHealthChecks();
        assertTrue(result.isHealthy());
        assertEquals("{\"runtimeStatus\":\"ok\",\"repositoryStatus\":\"ok\",\"testProbeStatus\":\"ok\"}",
                result.toJson());
    }

    /**
     * Non-regression test for NXP-24360
     *
     * @since 10.1
     */
    @Test
    public void testChangingHealthCheck() throws IOException, InterruptedException {
        // make sure test probe status should be ok
        assertTrue(fs.getStatus().isSuccess());
        HealthCheckResult result = pm.getOrRunHealthChecks();
        assertTrue(result.isHealthy());
        assertEquals("{\"runtimeStatus\":\"ok\",\"repositoryStatus\":\"ok\",\"testProbeStatus\":\"ok\"}",
                result.toJson());

        // make test probe return a failure status instead
        fs.setFailure();
        assertTrue(fs.getStatus().isFailure());
        result = pm.getOrRunHealthChecks();
        assertFalse(result.isHealthy());
        assertEquals("{\"runtimeStatus\":\"ok\",\"repositoryStatus\":\"ok\",\"testProbeStatus\":\"failed\"}",
                result.toJson());

        // make test probe status back to ok
        fs.setSuccess();
        assertTrue(fs.getStatus().isSuccess());
        result = pm.getOrRunHealthChecks();
        assertTrue(result.isHealthy());
        assertEquals("{\"runtimeStatus\":\"ok\",\"repositoryStatus\":\"ok\",\"testProbeStatus\":\"ok\"}",
                result.toJson());

        // make test probe throw an exception instead
        fs.setThrowException();
        try {
            fs.getStatus().isFailure();
            fail("should have thrown an exception");
        } catch (IllegalArgumentException e) {
            // ok
        }
        result = pm.getOrRunHealthChecks();
        assertFalse(result.isHealthy());
        assertEquals("{\"runtimeStatus\":\"ok\",\"repositoryStatus\":\"ok\",\"testProbeStatus\":\"failed\"}",
                result.toJson());

        // make test probe status back to ok
        fs.setSuccess();
        assertTrue(fs.getStatus().isSuccess());
        result = pm.getOrRunHealthChecks();
        assertTrue(result.isHealthy());
        assertEquals("{\"runtimeStatus\":\"ok\",\"repositoryStatus\":\"ok\",\"testProbeStatus\":\"ok\"}",
                result.toJson());
    }

    @Test
    public void testSingleProbeStatus() throws IOException {

        HealthCheckResult result = pm.getOrRunHealthCheck("runtimeStatus");
        ProbeInfo probeInfo = pm.getProbeInfo("runtimeStatus");

        assertTrue(result.isHealthy());
        assertTrue(probeInfo.getStatus().isSuccess());
        assertEquals("{\"runtimeStatus\":\"ok\"}", result.toJson());
    }

    /**
     * Non-regression test for NXP-24360
     *
     * @since 10.1
     */
    @Test
    public void testChangingSingleProbeStatus() throws IOException, InterruptedException {
        // make sure test probe status should be ok
        assertTrue(fs.getStatus().isSuccess());

        HealthCheckResult result = pm.getOrRunHealthCheck("testProbeStatus");
        ProbeInfo probeInfo = pm.getProbeInfo("testProbeStatus");
        assertTrue(result.isHealthy());
        assertTrue(probeInfo.getStatus().isSuccess());
        assertEquals("{\"testProbeStatus\":\"ok\"}", result.toJson());

        // make test probe throw an exception instead
        fs.setThrowException();
        try {
            fs.getStatus().isFailure();
            fail("should have thrown an exception");
        } catch (IllegalArgumentException e) {
            // ok
        }
        result = pm.getOrRunHealthCheck("testProbeStatus");
        probeInfo = pm.getProbeInfo("testProbeStatus");
        assertFalse(result.isHealthy());
        assertFalse(probeInfo.getStatus().isSuccess());
        assertEquals("{\"testProbeStatus\":\"failed\"}", result.toJson());

        // make test probe return a failure status instead
        fs.setFailure();
        assertTrue(fs.getStatus().isFailure());
        result = pm.getOrRunHealthCheck("testProbeStatus");
        probeInfo = pm.getProbeInfo("testProbeStatus");
        assertFalse(result.isHealthy());
        assertFalse(probeInfo.getStatus().isSuccess());
        assertEquals("{\"testProbeStatus\":\"failed\"}", result.toJson());

        // make test probe status back to ok
        fs.setSuccess();
        assertTrue(fs.getStatus().isSuccess());
        result = pm.getOrRunHealthCheck("testProbeStatus");
        probeInfo = pm.getProbeInfo("testProbeStatus");
        assertTrue(result.isHealthy());
        assertTrue(probeInfo.getStatus().isSuccess());
        assertEquals("{\"testProbeStatus\":\"ok\"}", result.toJson());
    }

    @Test
    public void testInvalidProbe() {
        IllegalArgumentException e = null;
        try {
            pm.getOrRunHealthCheck("invalidProbe");
        } catch (IllegalArgumentException e1) {
            e = e1;
        }
        assertNotNull(e);
    }

    @Test
    public void testRepositoryStatusProbe() throws IOException {

        HealthCheckResult result = pm.getOrRunHealthCheck("repositoryStatus");
        ProbeInfo probeInfo = pm.getProbeInfo("repositoryStatus");
        assertTrue(result.isHealthy());
        assertTrue(probeInfo.getStatus().isSuccess());
        assertEquals("{\"repositoryStatus\":\"ok\"}", result.toJson());
    }
}

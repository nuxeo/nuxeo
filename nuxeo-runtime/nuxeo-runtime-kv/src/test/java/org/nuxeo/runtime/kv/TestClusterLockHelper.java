/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.kv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * Test of the cluster lock service.
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ TestClusterLockHelper.ClusterFeature.class, RuntimeFeature.class, LogFeature.class,
        LogCaptureFeature.class })
@Deploy("org.nuxeo.runtime.kv")
@LocalDeploy("org.nuxeo.runtime.kv.tests:OSGI-INF/test-cluster-lock.xml")
public class TestClusterLockHelper {

    public static class ClusterFeature extends SimpleFeature {

        @Override
        public void start(FeaturesRunner runner) {
            Framework.addListener(new RuntimeServiceListener() {

                @Override
                public void handleEvent(RuntimeServiceEvent event) {
                    if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_START) {
                        return;
                    }
                    Framework.removeListener(this);
                    setClusterId();
                }
            });
        }

        public static void setClusterId() {
            Framework.getProperties().put(ClusterLockHelper.CLUSTERING_ENABLED_PROP, "true");
            Framework.getProperties().put(ClusterLockHelper.NODE_ID_PROP, "123");
        }
    }

    @Inject
    protected LogFeature logFeature;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Inject
    protected KeyValueService keyValueService;

    protected KeyValueStore kvStore;

    @Before
    public void setUp() {
        kvStore = keyValueService.getKeyValueStore(ClusterLockHelper.KV_STORE_NAME);
    }

    @After
    public void tearDown() {
        kvStore.put("mykey", (String) null);
    }

    @Test
    public void testLockFailure() {
        // simulate lock set on other node
        kvStore.put("mykey", "mylockinfo");
        // check that locking fails
        try {
            runAtomically(() -> {
            });
            fail();
        } catch (RuntimeServiceException e) {
            assertEquals("Failed to acquire lock 'mykey' after 3s, owner: mylockinfo", e.getMessage());
        }
    }

    @Test
    public void testLockOk() {
        MutableObject<String> lockInfo = new MutableObject<>();
        runAtomically(() -> {
            lockInfo.setValue(kvStore.getString("mykey"));
        });
        assertTrue(lockInfo.getValue(), Pattern.matches("node=123 time=.*", lockInfo.getValue()));
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN")
    public void testLockExpire() {
        logFeature.hideErrorFromConsoleLog();
        try {
            runAtomically(() -> {
                // simulate task taking too long and lock TTL expiring
                kvStore.put("mykey", (String) null);
            });
        } finally {
            logFeature.restoreConsoleLog();
        }
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(1, events.size());
        LoggingEvent event = events.get(0);
        assertEquals("WARN", event.getLevel().toString());
        assertEquals(
                "Unlocking 'mykey' but the lock had already expired; consider increasing the try duration for this lock",
                event.getMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "ERROR")
    public void testLockExpireThenStolen() {
        logFeature.hideErrorFromConsoleLog();
        try {
            runAtomically(() -> {
                // simulate task taking too long and lock TTL expiring and reacquired by someone else
                kvStore.put("mykey", (String) "node=456 time=sometime");
            });
        } finally {
            logFeature.restoreConsoleLog();
        }
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(1, events.size());
        LoggingEvent event = events.get(0);
        assertEquals("ERROR", event.getLevel().toString());
        assertEquals(
                "Failed to unlock 'mykey', the lock expired and has a new owner: node=456 time=sometime; consider increasing the try duration for this lock",
                event.getMessage());
    }

    protected void runAtomically(Runnable runnable) {
        Duration duration = Duration.ofSeconds(3); // short enough to be acceptable for tests
        Duration pollDelay = Duration.ofMillis(250);
        ClusterLockHelper.runAtomically("mykey", duration, pollDelay, runnable);
    }

}

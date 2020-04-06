/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.tests.computation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.lib.stream.tests.TestLibChronicle.IS_WIN;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.log.LogStreamManager;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;

import net.jodah.failsafe.RetryPolicy;

/**
 * @since 9.3
 */
public class TestLogStreamProcessorChronicle extends TestStreamProcessor {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected File basePath;

    @Before
    public void skipWindowsThatDoNotCleanTempFolder() {
        org.junit.Assume.assumeFalse(IS_WIN);
    }

    @Override
    public LogManager getLogManager() throws Exception {
        this.basePath = folder.newFolder();
        return new ChronicleLogManager(basePath.toPath());
    }

    @Override
    public LogManager getSameLogManager() {
        return new ChronicleLogManager(basePath.toPath());
    }

    // This test is based on static counter and should not be run multiple times
    // run it only with CQ impl
    @Test
    public void testComputationRecoveryPolicy() throws Exception {
        // Define a topology that fails
        Topology topology = Topology.builder()
                                    .addComputation(() -> new ComputationFailureForward("C1", 1, 1),
                                            Arrays.asList("i1:input", "o1:output"))
                                    .build();
        // Policy no retry, abort on failure but skip the first failure
        ComputationPolicy policy = new ComputationPolicyBuilder().retryPolicy(
                new RetryPolicy(ComputationPolicy.NO_RETRY)).continueOnFailure(false).skipFirstFailures(1).build();
        try (LogManager manager = getLogManager()) {
            StreamManager streamManager = new LogStreamManager(manager);
            Settings settings = new Settings(1, 1, policy);
            StreamProcessor processor = streamManager.registerAndCreateProcessor("processor", topology, settings);
            processor.start();
            processor.waitForAssignments(Duration.ofSeconds(10));
            // Add 2 records
            streamManager.append("input", Record.of("foo", null));
            streamManager.append("input", Record.of("bar", null));
            // wait
            assertTrue(processor.drainAndStop(Duration.ofSeconds(20)));
            LogLag lag = manager.getLag(Name.ofUrn("input"), Name.ofUrn("C1"));
            // only the first record is skipped after failure
            assertEquals(lag.toString(), 1, lag.lag());
        }
    }
}

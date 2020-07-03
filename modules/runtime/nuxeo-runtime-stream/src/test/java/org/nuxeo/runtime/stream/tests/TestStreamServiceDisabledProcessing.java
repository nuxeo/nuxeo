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
 *     bdelbosc
 */
package org.nuxeo.runtime.stream.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.stream.DisableStreamProcessingFeature;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ DisableStreamProcessingFeature.class, RuntimeStreamFeature.class })
@Deploy("org.nuxeo.runtime.stream:test-stream-contrib.xml")
public class TestStreamServiceDisabledProcessing {

    @Inject
    public StreamService service;

    @Test
    public void testStreamProcessor() throws Exception {
        @SuppressWarnings("resource")
        LogManager manager = service.getLogManager();

        // Streams defined in processor are initialized
        assertTrue(manager.exists(Name.ofUrn("input")));
        assertTrue(manager.exists(Name.ofUrn("s1")));
        assertTrue(manager.exists(Name.ofUrn("output")));

        // We can append record to input streams
        StreamManager streamManager = service.getStreamManager();
        streamManager.append("input", Record.of("Hi", null));

        // But there is no computation thread to run the processing
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        for (Thread thread : threads) {
            if (thread.getName().startsWith("myComputation")) {
                fail("A computation thread has been found: " + threads);
            }
        }
    }

}

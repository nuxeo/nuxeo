/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.management.metrics;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.javasimon.Sample;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class MetricSerializerTestCase {

    final MetricSerializer srv = new MetricSerializer();

    static class Context {
        public String getInfo() {
            return "info";
        }
    }

    Sample newSample() {
        try {
            Stopwatch stopwatch = SimonManager.getStopwatch("test");
            stopwatch.setAttribute("ctx", new Context());
            Split split = stopwatch.start();
            split.stop();
            return stopwatch.sample();
        } finally {
            SimonManager.destroySimon("test");
        }
    }

    @Before
    public void enableManager() {
        SimonManager.enable();
    }

    @After
    public void disableManager() {
        SimonManager.disable();
    }

    @Test
    public void testService() throws IOException {
        srv.resetOutput();
        srv.toStream(newSample());
        srv.flushOuput();
        assertTrue(srv.getOutputFile().length() > 0);
    }

}

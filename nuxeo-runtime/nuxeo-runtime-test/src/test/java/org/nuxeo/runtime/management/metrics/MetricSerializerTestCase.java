/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
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

/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.impl.ComponentManagerImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestExtensionPointWithError extends NXRuntimeTestCase {

    protected List<Appender> savedAppenders;

    protected ArrayList<LoggingEvent> loggingEvents = new ArrayList<>();

    protected Appender appender = new AppenderSkeleton() {
        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        public void close() {
        }

        /** Keep only logging events for ComponentManagerImpl. */
        @Override
        protected void append(LoggingEvent event) {
            if (ComponentManagerImpl.class.getName().equals(event.getLoggerName())) {
                loggingEvents.add(event);
            }
        }
    };

    @Override
    protected void setUp() throws Exception {
        deployContrib("org.nuxeo.runtime.test.tests", "BaseXPoint.xml");

        // save appenders
        Logger rootLogger = Logger.getRootLogger();
        @SuppressWarnings("unchecked")
        Enumeration<Appender> rootAppenders = rootLogger.getAllAppenders();
        savedAppenders = Collections.list(rootAppenders);

        // intercept all logs to avoid expected logging on Console
        rootLogger.removeAllAppenders();
        rootLogger.addAppender(appender);
        loggingEvents.clear();

        // add contribution with error
        deployContrib("org.nuxeo.runtime.test.tests", "OverridingXPoint-witherror.xml");
    }

    @Override
    protected void tearDown() {
        // restore appenders
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.removeAllAppenders();
        for (Appender appender : savedAppenders) {
            rootLogger.addAppender(appender);
        }
    }

    @Test
    public void testInvalidExtensionPoint() throws Exception {
        ComponentWithXPoint co = (ComponentWithXPoint) Framework.getRuntime().getComponent(ComponentWithXPoint.NAME);
        DummyContribution[] contribs = co.getContributions();
        assertEquals(0, contribs.length); // contrib with errors not loaded

        // check runtime warnings
        List<String> warnings = Framework.getRuntime().getWarnings();
        assertEquals(1, warnings.size());
        assertEquals("Failed to load contributions for component service:OverridingXPoint-witherror", warnings.get(0));

        // check logs
        assertEquals(1, loggingEvents.size());
        LoggingEvent event = loggingEvents.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals("Failed to load contributions for component service:OverridingXPoint-witherror", event.getRenderedMessage());
        Throwable t = event.getThrowableInformation().getThrowable();
        assertEquals(RuntimeException.class, t.getClass());
        assertEquals("Cannot load class: this-is-not-a-class while processing component: OverridingXPoint-witherror", t.getMessage());
    }

}

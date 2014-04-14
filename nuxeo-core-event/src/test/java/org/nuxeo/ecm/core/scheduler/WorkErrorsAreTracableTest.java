/*******************************************************************************
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.nuxeo.ecm.core.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkSchedulePath;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.NoLogCaptureFilterException;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogCaptureFeature.class })
@Deploy({ "org.nuxeo.ecm.core.event" })
@LogCaptureFeature.FilterWith(WorkErrorsAreTracableTest.ChainFilter.class)
public class WorkErrorsAreTracableTest {

    protected static class Fail extends AbstractWork {

        private static final long serialVersionUID = 1L;

        @Override
        public String getTitle() {
            return Nest.class.getSimpleName();
        }

        @Override
        public void work() throws Exception {
            throw new Error();
        }

        public static class Error extends java.lang.Error {

            private static final long serialVersionUID = 1L;

        }
    }

    protected class Nest extends AbstractWork {

        /**
         *
         */
        private static final long serialVersionUID = 1L;
        protected Work sub;

        @Override
        public String getTitle() {
            return Nest.class.getSimpleName();
        }

        @Override
        public void work() throws Exception {
            sub = new Fail();
            manager.schedule(sub);
        }

    }

    public static class ChainFilter implements LogCaptureFeature.Filter {

        @Override
        public boolean accept(LoggingEvent event) {
            String category = event.getLogger().getName();
            return WorkSchedulePath.class.getName().equals(category);
        }

    }

    @Inject
    protected WorkManager manager;

    @Inject
    protected LogCaptureFeature.Result result;

    protected boolean beforeCapturePath;

    @Before
    public void captureStacks() {
        beforeCapturePath = WorkSchedulePath.capturePath;
        if (!WorkSchedulePath.isCaptureStackEnabled()) {
            WorkSchedulePath.toggleCaptureStack();
        }
    }

    @After
    public void resetCaptureStacks() {
        if (!beforeCapturePath) {
            WorkSchedulePath.toggleCapturePath();
        }
    }

    @Test
    public void captureSimple() throws InterruptedException, NoLogCaptureFilterException {
        Fail work = new Fail();
        manager.schedule(work);
        WorkSchedulePath.Trace error = awaitFailure(work);
        assertNotNull(error);
    }

    @Test
    public void captureChained() throws InterruptedException, NoLogCaptureFilterException {
        Nest work = new Nest();
        manager.schedule(work);
        WorkSchedulePath.Trace error = awaitFailure(work);
        WorkSchedulePath.Trace cause = (WorkSchedulePath.Trace) error.getCause();
        assertEquals(work.getSchedulePath(), cause.path());
    }

    protected WorkSchedulePath.Trace awaitFailure(Work work)
            throws InterruptedException, NoLogCaptureFilterException {
        boolean completed = manager.awaitCompletion(1000, TimeUnit.MILLISECONDS);
        assertTrue(completed);
        result.assertHasEvent();
        LoggingEvent loggingEvent = result.getCaughtEvents().get(0);
        WorkSchedulePath.Trace trace = (WorkSchedulePath.Trace)loggingEvent.getThrowableInformation().getThrowable();
        assertIsRootWork(work, trace);
        return trace;
    }

    protected void assertIsRootWork(Work work, WorkSchedulePath.Trace error) {
        for (Throwable cause = error.getCause(); cause != null
                && cause != error; error = (WorkSchedulePath.Trace) cause) {
            ;
        }
        assertEquals(work.getSchedulePath(), error.path());
    }

}

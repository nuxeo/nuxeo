/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.logging.log4j.core.LogEvent;
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
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogCaptureFeature.class })
@Deploy("org.nuxeo.ecm.core.event")
@LogCaptureFeature.FilterOn(loggerClass = WorkSchedulePath.class)
public class WorkErrorsAreTracableTest {

    protected static class Fail extends AbstractWork {

        private static final long serialVersionUID = 1L;

        @Override
        public String getTitle() {
            return Nest.class.getSimpleName();
        }

        @Override
        public void work() {
            throw new RuntimeException("test");
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
        public void work() {
            sub = new Fail();
            manager.schedule(sub);
        }

    }

    @Inject
    protected WorkManager manager;

    @Inject
    protected LogCaptureFeature.Result result;

    @Test
    public void captureSimple() throws InterruptedException {
        Fail work = new Fail();
        manager.schedule(work);
        WorkSchedulePath.Trace error = awaitFailure(work);
        assertNotNull(error);
    }

    @Test
    public void captureChained() throws InterruptedException {
        Nest work = new Nest();
        manager.schedule(work);
        WorkSchedulePath.Trace error = awaitFailure(work);
        WorkSchedulePath.Trace cause = (WorkSchedulePath.Trace) error.getCause();
        assertEquals(work.getSchedulePath(), cause.path());
    }

    protected WorkSchedulePath.Trace awaitFailure(Work work) throws InterruptedException {
        boolean completed = manager.awaitCompletion(1000, TimeUnit.MILLISECONDS);
        assertTrue(completed);
        result.assertHasEvent();
        LogEvent loggingEvent = result.getCaughtEvents().get(0);
        WorkSchedulePath.Trace trace = (WorkSchedulePath.Trace) loggingEvent.getThrown();
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

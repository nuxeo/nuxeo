/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.NoLogCaptureFilterException;
import org.slf4j.MDC;

/**
 * @author matic
 */
@RunWith(FeaturesRunner.class)
@Features({ JtajcaManagementFeature.class, CoreFeature.class, LogCaptureFeature.class })
public class CanMonitorTransactionsTest {

    @Inject
    @Named("default")
    protected TransactionMonitor monitor;

    @Inject
    protected TransactionManager tm;

    protected ExecutorService executor;

    @Before
    public void injectExectutor() {
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void shudownExecutor() {
        executor.shutdownNow();
    }

    protected void begin() {
        try {
            tm.begin();
        } catch (Exception cause) {
            throw new RuntimeException("Cannot start new transacton", cause);
        }
    }

    protected void rollback() {
        try {
            tm.rollback();
        } catch (Exception cause) {
            throw new RuntimeException("Cannot rollback transaction", cause);
        }
    }

    protected void commit() {
        try {
            tm.commit();
        } catch (Exception cause) {
            throw new RuntimeException("Cannot commit transaction", cause);
        }
    }

    @Test
    public void isMonitorInstalled() {
        assertThat(monitor, notNullValue());
        monitor.getTotalCommits(); // throw exception is monitor not present
    }

    protected class TestTotalRollbacks implements Callable<Boolean> {

        @Override
        public Boolean call() {
            long activeCount = monitor.getActiveCount();
            long totalRollbacks = monitor.getTotalRollbacks();
            begin();
            assertThat(monitor.getActiveCount(), is(activeCount + 1));
            rollback();
            assertThat(monitor.getActiveCount(), is(activeCount));
            assertThat(monitor.getTotalRollbacks(), is(totalRollbacks + 1));

            return Boolean.TRUE;
        }

    }

    @Test
    public void isTotalRollbacksCorrect() throws InterruptedException, ExecutionException {
        assertThat(monitor.getActiveCount(), is(1L));
        FutureTask<Boolean> rollback = new FutureTask<>(new TestTotalRollbacks());
        executor.execute(rollback);
        assertThat(rollback.get(), is(true));
    }

    protected class TestTotalCommits implements Callable<Boolean> {

        @Override
        public Boolean call() {
            long totalCommits = monitor.getTotalCommits();
            begin();
            commit();
            assertThat(monitor.getTotalCommits(), is(totalCommits + 1));
            return Boolean.TRUE;
        }

    }

    @Test
    public void isTotalCommitsCorrect() throws InterruptedException, ExecutionException {
        FutureTask<Boolean> commit = new FutureTask<>(new TestTotalCommits());
        executor.execute(commit);
        assertThat(commit.get(), is(true));
    }

    protected class TestCollectStatistics implements Callable<Boolean> {

        @Override
        public Boolean call() {
            try {
                List<TransactionStatistics> stats = monitor.getActiveStatistics();
                long count = stats.size();
                begin();
                stats = monitor.getActiveStatistics();
                assertThat((long) stats.size(), is(count + 1));
                commit();
                stats = monitor.getActiveStatistics();
                assertThat((long) stats.size(), is(count));
            } catch (Exception cause) {
                LogFactory.getLog(CanMonitorTransactionsTest.class).error("Caught error while collecting statistics",
                        cause);
            }

            return Boolean.TRUE;
        }

    }

    @Test
    public void isActiveStatisticsCollected() throws InterruptedException, ExecutionException {
        FutureTask<Boolean> task = new FutureTask<>(new TestCollectStatistics());
        executor.execute(task);
        assertThat(task.get(), is(true));
    }

    @Inject
    LogCaptureFeature.Result logCaptureResults;

    @Test
    @LogCaptureFeature.FilterWith(value = CanMonitorTransactionsTest.LogRollbackTraceFilter.class)
    public void logContainsRollbackTrace() throws InterruptedException, ExecutionException, NoLogCaptureFilterException {
        FutureTask<Boolean> task = new FutureTask<>(new TestLogRollbackTrace());
        executor.execute(task);
        assertThat(task.get(), is(true));
        logCaptureResults.assertHasEvent();
    }

    protected class TestLogRollbackTrace implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            begin();
            rollback();
            return true;
        }
    }

    public static class LogRollbackTraceFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LoggingEvent event) {
            if (event.getLevel() != Level.TRACE) {
                return false;
            }
            Object msg = event.getMessage();
            if (!(msg instanceof TransactionStatistics)) {
                return false;
            }
            TransactionStatistics stats = (TransactionStatistics) msg;
            if (!TransactionStatistics.Status.ROLLEDBACK.equals(stats.getStatus())) {
                return false;
            }
            return true;
        }
    }

    @Test
    @LogCaptureFeature.FilterWith(value = CanMonitorTransactionsTest.LogMessageFilter.class)
    public void logContainsTxKey() throws InterruptedException, ExecutionException, NoLogCaptureFilterException {
        FutureTask<Boolean> task = new FutureTask<>(new TestLogRollbackTrace());
        executor.execute(task);
        assertThat(task.get(), is(true));
        logCaptureResults.assertHasEvent();
    }

    public static class LogMessageFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LoggingEvent event) {
            return MDC.get("tx") != null;
        }
    }

    protected class TestLogMessage implements Callable<Boolean> {
        protected final Log log = LogFactory.getLog(TestLogMessage.class);

        @Override
        public Boolean call() throws Exception {
            begin();
            log.warn("logging with active tx");
            rollback();
            return true;
        }
    }

}

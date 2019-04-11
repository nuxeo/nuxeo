/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.management.jtajca;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.IgnoreNonPooledCondition;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManager;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.name.Named;

/**
 * Active connections are monitored and killed when their duration exceed
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features({JtajcaManagementFeature.class, CoreFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class)
@ConditionalIgnoreRule.Ignore(condition = IgnoreNonPooledCondition.class)
public class CanCleanupConnectionLeakTest {

    @Inject
    @Named("repository/test")
    ConnectionPoolMonitor monitor;

    @Inject
    CoreFeature core;

    final Lock lock = new ReentrantLock();

    final Condition timedout = lock.newCondition();

    final Condition killed = lock.newCondition();

    abstract class Task implements Runnable {
        abstract void work() throws InterruptedException;

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(getClass().getSimpleName());
            try {
                try {
                    lock.lock();
                    try {
                        work();
                    } catch (InterruptedException cause) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } finally {
                    lock.unlock();
                }
            } finally {
                Thread.currentThread().setName(name);
            }
        }
    }

    class OpenTask extends Task {

        @Override
        void work() throws InterruptedException {
            TransactionHelper.startTransaction();
            try {
                NuxeoConnectionManager mgr = NuxeoContainer.getConnectionManager(monitor.getName());
                mgr.enterActiveMonitor(1);
                try (CloseableCoreSession session = core.openCoreSession()) {
                    session.getDocument(new PathRef("/"));
                    Thread.sleep(10);
                    timedout.signal();
                    Assertions.assertThat(killed.await(10, TimeUnit.SECONDS)).isTrue();
                    session.getDocument(new PathRef("/default-domain"));
                } finally {
                    mgr.exitActiveTimedout();
                }
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
            }

        }
    }

    class KillTask extends Task {
        @Override
        void work() throws InterruptedException {
            Assertions.assertThat(monitor.getKilledActiveConnectionCount()).isEqualTo(0);
            Assertions.assertThat(timedout.await(10,TimeUnit.SECONDS)).isTrue();
            try {
                Assertions.assertThat(monitor.killActiveTimedoutConnections()).isEqualTo(1);
            } finally {
                killed.signal();
            }
            Assertions.assertThat(monitor.getKilledActiveConnectionCount()).isEqualTo(1);
        }
    }

    @Ignore("NXP-21638")
    @Test
    public void openAndKill() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            Future<?> killstate = executor.submit(new KillTask());
            Future<?> openstate = executor.submit(new OpenTask());
            try {
                openstate.get();
            } catch (ExecutionException executionError) {
                Throwable cause = executionError.getCause();
                if (!(cause instanceof IllegalStateException) && !cause.getMessage().contains("unknown connection")) {
                    throw new AssertionError("Caught an unkown error from session holder", cause);
                }
                killstate.get();
                return;
            }
            killstate.get();
            throw new AssertionError("Didn't caught connection error");
        } finally {
            executor.shutdownNow();
        }
    }
}

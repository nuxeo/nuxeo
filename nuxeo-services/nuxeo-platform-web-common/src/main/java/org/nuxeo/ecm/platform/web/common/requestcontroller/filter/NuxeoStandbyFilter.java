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
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;

/**
 * Blocks incoming requests when runtime is in standby mode.
 *
 * @since 9.2
 */
public class NuxeoStandbyFilter implements Filter {

    protected Controller controller;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        controller = new Controller();
        new ComponentManager.Listener() {

            @Override
            public void beforeStop(ComponentManager mgr, boolean isStandby) {
                controller.onStandby();
            }

            @Override
            public void afterStart(ComponentManager mgr, boolean isResume) {
                controller.onResumed();
            }

        }.install();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        controller.onNewRequest();
        try {
            chain.doFilter(request, response);
        } finally {
            controller.onRequestEnd();
        }
    }

    @Override
    public void destroy() {

    }

    protected class Controller {
        protected final Lock lock = new ReentrantLock();

        protected final Condition canStandby = lock.newCondition();

        protected final Condition canProceed = lock.newCondition();

        protected volatile boolean isStandby = !Framework.getRuntime().getComponentManager().isStarted();

        protected final AtomicInteger inProgress = new AtomicInteger();

        /**
         * This variable is used to determine if the Thread wanting to shutdown/standby the server has gone through this
         * filter. We need this variable in order to not wait for ourself to end.
         * <p />
         * Calls relying on this variable:
         * <ul>
         * <li>org.nuxeo.runtime.reload.NuxeoRestart#restart()</li>
         * <li>org.nuxeo.ecm.admin.operation.HotReloadStudioSnapshot#run()</li>
         * <li>org.nuxeo.connect.client.jsf.AppCenterViewsManager#installStudioSnapshotAndRedirect()</li>
         * </ul>
         */
        protected final ThreadLocal<Boolean> hasBeenFiltered = ThreadLocal.withInitial(() -> Boolean.FALSE);

        public void onNewRequest() {
            if (isStandby) {
                awaitCanProceed();
            }
            inProgress.incrementAndGet();
            hasBeenFiltered.set(Boolean.TRUE);
        }

        public void onRequestEnd() {
            hasBeenFiltered.set(Boolean.FALSE);
            if (inProgress.decrementAndGet() <= 0) {
                signalBlockedToStandby();
            }
        }

        public void onStandby() throws RuntimeException {
            isStandby = true;
            if (hasBeenFiltered.get()) {
                // current thread has gone through this filter, so remove this request from counter
                // otherwise we will wait for ourself to end
                inProgress.decrementAndGet();
            }
            if (inProgress.get() > 0) {
                awaitCanStandby();
            }
        }

        public void onResumed() {
            isStandby = false;
            if (hasBeenFiltered.get()) {
                // current thread has gone through this filter, so add this request back to counter
                // as we removed this request just before / proceeding as it makes conditions easier to read
                inProgress.incrementAndGet();
            }
            signalBlockedToProceed();
        }

        protected void awaitCanProceed() throws RuntimeException {
            lock.lock();
            try {
                canProceed.await();
            } catch (InterruptedException cause) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while locking incoming requests", cause);
            } finally {
                lock.unlock();
            }
        }

        protected void awaitCanStandby() throws RuntimeException {
            lock.lock();
            try {
                canStandby.await();
            } catch (InterruptedException cause) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for web requests being drained", cause);
            } finally {
                lock.unlock();
            }
        }

        protected void signalBlockedToProceed() {
            lock.lock();
            try {
                canProceed.signalAll();
            } finally {
                lock.unlock();
            }
        }

        protected void signalBlockedToStandby() {
            lock.lock();
            try {
                canStandby.signal();
            } finally {
                lock.unlock();
            }
        }

    }

}

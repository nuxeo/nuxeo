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
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;

/**
 * Blocks incoming requests when runtime is in standby mode.
 *
 * @since 9.2
 */
public class NuxeoStandbyFilter implements Filter {

    protected final Lock lock = new ReentrantLock();

    protected final Condition resumed = lock.newCondition();

    protected final Filter locker = new Filter() {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            lock.lock();
            try {
                resumed.await();
            } catch (InterruptedException cause) {
                throw new ServletException("Interrupted while waiting for resume", cause);
            } finally {
                lock.unlock();
            }
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }
    };

    protected volatile boolean isStandby = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        isStandby = Framework.getRuntime().isStandby();
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id == RuntimeServiceEvent.RUNTIME_ABOUT_TO_STANDBY) {
                    isStandby = true;
                } else if (event.id == RuntimeServiceEvent.RUNTIME_RESUMED) {
                    isStandby = false;
                    lock.lock();
                    try {
                        resumed.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
            }
        });
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (isStandby) {
            awaitOnResume();
        }
        chain.doFilter(request, response);
    }

    protected void awaitOnResume()  {
        lock.lock();
        if (!isStandby) {
            return;
        }
        try {
            resumed.await();
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while locking incoming requests", cause);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void destroy() {

    }

}

/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.management.jvm.ThreadDeadlocksDetector;

public class ThreadDeadlocksRunner {

    protected Log log = LogFactory.getLog(ThreadDeadlocksRunner.class);

    protected Object mgr = new Object();

    protected Object repo = new Object();

    protected boolean isMgrOwned = false;

    protected boolean isRepoOwned = false;

    public void run() {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (ThreadDeadlocksRunner.this) {
                        if (!isRepoOwned) {
                            ThreadDeadlocksRunner.this.wait();
                        }
                    }
                    if (isRepoOwned == false) {
                        throw new RuntimeException("Repo not owned");
                    }
                    synchronized (mgr) {
                        isMgrOwned = true;
                        log.debug("thread:lock:mgr");
                        synchronized (repo) {
                            // dead locked, should not enter
                            log.debug("thread:lock:repo");
                        }
                        log.debug("thread:unlock:repo");
                    }
                    log.debug("thread:unlock:mgr");
                } catch (Throwable e) {
                    log.debug("thread:caught", e);
                }
            }
        };

        t.start();

        try {
            synchronized (repo) {
                log.debug("main:lock:repo");
                isRepoOwned = true;

                synchronized (this) {
                    this.notify();
                }

                if (isMgrOwned == false) {
                    synchronized (this) {
                        this.wait();
                    }
                }

                if (isMgrOwned == false) {
                    throw new RuntimeException("Mgr is not owned");
                }

                synchronized (mgr) {
                    // dead locked, should never enter
                    log.debug("main:lock:mgr");
                }
                log.debug("main:unlock:mgr");
            }
        } catch (Throwable e) {
            log.debug("main:caught", e);
        }

    }

    public static void main(String[] args) {

        final ThreadDeadlocksDetector detector = new ThreadDeadlocksDetector();

        detector.schedule(10 * 1000, new ThreadDeadlocksDetector.KillListener());

        new ThreadDeadlocksRunner().run();
    }
}

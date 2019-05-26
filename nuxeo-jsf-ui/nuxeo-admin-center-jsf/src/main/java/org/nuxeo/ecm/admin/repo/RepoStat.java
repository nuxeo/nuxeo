/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.admin.repo;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * This class holds and manage the threads used to compute stats on the document repository
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class RepoStat {

    protected final ThreadPoolExecutor pool;

    protected int nbThreads = 5;

    protected final String repoName;

    protected final boolean includeBlob;

    protected RepoStatInfo info;

    public RepoStat(String repoName, int nbThreads, boolean includeBlob) {
        this.nbThreads = nbThreads;
        this.repoName = repoName;
        this.includeBlob = includeBlob;
        pool = new ThreadPoolExecutor(nbThreads, nbThreads, 500L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(100), new DaemonThreadFactory());
    }

    public void exec(StatsTask task) {
        pool.execute(task);
    }

    public void run(DocumentRef root) {
        info = new RepoStatInfo();
        StatsTask task = new StatsTask(repoName, root, includeBlob, this);
        exec(task);
    }

    protected boolean isPoolFull() {
        return pool.getQueue().size() > 1;
    }

    public RepoStatInfo getInfo() {
        return info;
    }

    public boolean isRunning() {
        return pool.getActiveCount() > 0;
    }

    protected static class DaemonThreadFactory implements ThreadFactory {

        private final ThreadGroup group;

        private final String namePrefix;

        private static final AtomicInteger poolNumber = new AtomicInteger();

        private final AtomicInteger threadNumber = new AtomicInteger();

        public DaemonThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "RepoStatThread-" + poolNumber.incrementAndGet() + '-';
        }

        @Override
        public Thread newThread(Runnable r) {
            String name = namePrefix + threadNumber.incrementAndGet();
            Thread t = new Thread(group, r, name);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }

    }
}

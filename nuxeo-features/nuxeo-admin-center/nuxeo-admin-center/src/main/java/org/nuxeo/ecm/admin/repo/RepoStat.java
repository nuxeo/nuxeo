/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * This class holds and manage the threads used to compute stats on the
 * document repository
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
        pool = new ThreadPoolExecutor(nbThreads, nbThreads, 500L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100),new DaemonThreadFactory());
    }

    public void exec(StatsTask task) {
        pool.execute(task);
    }

    public void run(DocumentRef root) throws Exception {
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
            group = (s != null) ? s.getThreadGroup()
                    : Thread.currentThread().getThreadGroup();
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

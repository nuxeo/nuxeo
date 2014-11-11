/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.runtime.management.jvm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ThreadDeadlocksDetector {

    protected Timer timer;

    protected final ThreadMXBean mgmt = ManagementFactory.getThreadMXBean();

    protected final Printer printer = new JVM16Printer();

    protected static final Log log = LogFactory.getLog(ThreadDeadlocksDetector.class);

    public interface Printer {

        void print(final StringBuilder sb, final ThreadInfo thread);

        void printMonitors(final StringBuilder sb,
                final MonitorInfo[] monitors, final int index);

        void printLock(StringBuilder sb, LockInfo lock);

    }

    public static class JVM16Printer implements Printer {

        protected final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();

        @Override
        public void print(final StringBuilder sb, final ThreadInfo thread) {
            MonitorInfo[] monitors = null;
            if (mbean.isObjectMonitorUsageSupported()) {
                monitors = thread.getLockedMonitors();
            }
            sb.append("\n\"" + thread.getThreadName() + // NOI18N
                    "\" - Thread t@" + thread.getThreadId() + "\n"); // NOI18N
            sb.append("   java.lang.Thread.State: " + thread.getThreadState()); // NOI18N
            sb.append("\n"); // NOI18N
            int index = 0;
            for (StackTraceElement st : thread.getStackTrace()) {
                LockInfo lock = thread.getLockInfo();
                String lockOwner = thread.getLockOwnerName();

                sb.append("\tat " + st.toString() + "\n"); // NOI18N
                if (index == 0) {
                    if ("java.lang.Object".equals(st.getClassName()) && // NOI18N
                            "wait".equals(st.getMethodName())) { // NOI18N
                        if (lock != null) {
                            sb.append("\t- waiting on "); // NOI18N
                            printLock(sb, lock);
                            sb.append("\n"); // NOI18N
                        }
                    } else if (lock != null) {
                        if (lockOwner == null) {
                            sb.append("\t- parking to wait for "); // NOI18N
                            printLock(sb, lock);
                            sb.append("\n"); // NOI18N
                        } else {
                            sb.append("\t- waiting to lock "); // NOI18N
                            printLock(sb, lock);
                            sb.append(" owned by \"" + lockOwner + "\" t@"
                                    + thread.getLockOwnerId() + "\n"); // NOI18N
                        }
                    }
                }
                printMonitors(sb, monitors, index);
                index++;
            }
            StringBuilder jnisb = new StringBuilder();
            printMonitors(jnisb, monitors, -1);
            if (jnisb.length() > 0) {
                sb.append("   JNI locked monitors:\n");
                sb.append(jnisb);
            }
            if (mbean.isSynchronizerUsageSupported()) {
                sb.append("\n   Locked ownable synchronizers:"); // NOI18N
                LockInfo[] synchronizers = thread.getLockedSynchronizers();
                if (synchronizers == null || synchronizers.length == 0) {
                    sb.append("\n\t- None\n"); // NOI18N
                } else {
                    for (LockInfo li : synchronizers) {
                        sb.append("\n\t- locked "); // NOI18N
                        printLock(sb, li);
                        sb.append("\n"); // NOI18N
                    }
                }
            }
        }

        @Override
        public void printMonitors(final StringBuilder sb,
                final MonitorInfo[] monitors, final int index) {
            if (monitors != null) {
                for (MonitorInfo mi : monitors) {
                    if (mi.getLockedStackDepth() == index) {
                        sb.append("\t- locked "); // NOI18N
                        printLock(sb, mi);
                        sb.append("\n"); // NOI18N
                    }
                }
            }
        }

        @Override
        public void printLock(StringBuilder sb, LockInfo lock) {
            String id = Integer.toHexString(lock.getIdentityHashCode());
            String className = lock.getClassName();

            sb.append("<" + id + "> (a " + className + ")"); // NOI18N
        }

    }

    public interface Listener {

        void deadlockDetected(long[] ids, File dumpFile);

    }

    public static class KillListener implements Listener {

        @Override
        public void deadlockDetected(long[] ids, File dumpFile) {
            log.error("Exiting, detected threads dead locks, see thread dump in " + dumpFile.getPath());
            System.exit(1);
        }

    }

     public File dump(long[] lockedIds)
            throws UnsupportedEncodingException, IOException {
        File file = File.createTempFile("tcheck-", ".tdump");
        FileOutputStream os = new FileOutputStream(file);
        ThreadInfo[] infos = mgmt.dumpAllThreads(true, true);
        try {
            for (ThreadInfo info : infos) {
                StringBuilder sb = new StringBuilder();
                printer.print(sb, info);
                os.write(sb.toString().getBytes("UTF-8"));
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Locked threads: ");


            String comma="";
            for(long lockedId:lockedIds) {
                sb.append(comma).append(lockedId);
                comma = ",";
            }
            os.write(sb.toString().getBytes("UTF-8"));
        } finally {
            os.close();
        }
        return file;
    }

    public long[] detectThreadLock() {
        return mgmt.findMonitorDeadlockedThreads();
    }

    protected class Task extends TimerTask {

        protected final Listener listener;

        protected Task(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            long[] ids = detectThreadLock();
            if (ids == null) {
                return;
            }
            File dumpFile;
            try {
                dumpFile = dump(ids);
            } catch (Exception e) {
                log.error("Cannot dump threads", e);
                dumpFile = new File("/dev/null");
            }
            listener.deadlockDetected(ids, dumpFile);
        }

    }

    public void schedule(long period, Listener listener) {
        if (timer != null) {
            throw new IllegalStateException("timer already scheduled");
        }
        timer = new Timer("Thread Deadlocks Detector");
        timer.schedule(new Task(listener), 1000, period);
    }

    public void cancel() {
        if (timer == null) {
            throw new IllegalStateException("timer not scheduled");
        }
        timer.cancel();
        timer = null;
    }

    @SuppressWarnings("deprecation")
    public static void killThreads(Set<Long> ids) {
        Map<Long, Thread> threads = getThreads();
        for (long id : ids) {
            Thread thread = threads.get(id);
            if (thread == null) {
                continue;
            }
            thread.stop();
        }
    }

    protected static Map<Long, Thread> getThreads() {
        ThreadGroup root = rootGroup(Thread.currentThread().getThreadGroup());
        int nThreads = root.activeCount();
        Thread[] threads = new Thread[2 * nThreads];
        root.enumerate(threads);
        Map<Long, Thread> map = new HashMap<Long, Thread>(threads.length);
        for (Thread thread : threads) {
            if (thread == null) {
                continue;
            }
            map.put(thread.getId(), thread);
        }
        return map;
    }

    protected static ThreadGroup rootGroup(ThreadGroup group) {
        ThreadGroup parent = group.getParent();
        if (parent == null) {
            return group;
        }
        return rootGroup(parent);
    }

}

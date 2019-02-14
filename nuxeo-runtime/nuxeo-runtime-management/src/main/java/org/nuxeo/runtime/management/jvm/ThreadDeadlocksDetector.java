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
 *     matic
 */
package org.nuxeo.runtime.management.jvm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

        void printMonitors(final StringBuilder sb, final MonitorInfo[] monitors, final int index);

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
            sb.append("\n\"")
              .append(thread.getThreadName())
              .append("\" - Thread t@")
              .append(thread.getThreadId())
              .append("\n")
              .append("   java.lang.Thread.State: ")
              .append(thread.getThreadState())
              .append("\n");
            int index = 0;
            for (StackTraceElement st : thread.getStackTrace()) {
                LockInfo lock = thread.getLockInfo();
                String lockOwner = thread.getLockOwnerName();

                sb.append("\tat ").append(st.toString()).append("\n");
                if (index == 0) {
                    if ("java.lang.Object".equals(st.getClassName()) &&
                            "wait".equals(st.getMethodName())) {
                        if (lock != null) {
                            sb.append("\t- waiting on ");
                            printLock(sb, lock);
                            sb.append("\n");
                        }
                    } else if (lock != null) {
                        if (lockOwner == null) {
                            sb.append("\t- parking to wait for ");
                            printLock(sb, lock);
                            sb.append("\n");
                        } else {
                            sb.append("\t- waiting to lock ");
                            printLock(sb, lock);
                            sb.append(" owned by \"")
                              .append(lockOwner)
                              .append("\" t@")
                              .append(thread.getLockOwnerId())
                              .append("\n");
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
                sb.append("\n   Locked ownable synchronizers:");
                LockInfo[] synchronizers = thread.getLockedSynchronizers();
                if (synchronizers == null || synchronizers.length == 0) {
                    sb.append("\n\t- None\n");
                } else {
                    for (LockInfo li : synchronizers) {
                        sb.append("\n\t- locked ");
                        printLock(sb, li);
                        sb.append("\n");
                    }
                }
            }
        }

        @Override
        public void printMonitors(final StringBuilder sb, final MonitorInfo[] monitors, final int index) {
            if (monitors != null) {
                for (MonitorInfo mi : monitors) {
                    if (mi.getLockedStackDepth() == index) {
                        sb.append("\t- locked ");
                        printLock(sb, mi);
                        sb.append("\n");
                    }
                }
            }
        }

        @Override
        public void printLock(StringBuilder sb, LockInfo lock) {
            String id = Integer.toHexString(lock.getIdentityHashCode());
            String className = lock.getClassName();
            sb.append("<").append(id).append("> (a ").append(className).append(")");
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

    public File dump(long[] lockedIds) throws IOException {
        File file = File.createTempFile("tdump-", ".log", new File("target"));
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

            String comma = "";
            for (long lockedId : lockedIds) {
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
        long[] findMonitorDeadlockedThreads = mgmt.findMonitorDeadlockedThreads();
        if (findMonitorDeadlockedThreads == null) {
            return new long[0];
        }
        return findMonitorDeadlockedThreads;
    }

    protected class Task extends TimerTask {

        protected final Listener listener;

        protected Task(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            long[] ids = detectThreadLock();
            if (ids.length == 0) {
                return;
            }
            File dumpFile;
            try {
                dumpFile = dump(ids);
            } catch (IOException e) {
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
        Map<Long, Thread> map = new HashMap<>(threads.length);
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

package org.nuxeo.osgi.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public  class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger();

    private final ThreadGroup group;

    private final String prefix;

    public NamedThreadFactory(String prefix) {
        SecurityManager sm = System.getSecurityManager();
        group = sm == null ? Thread.currentThread().getThreadGroup()
                : sm.getThreadGroup();
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        String name = prefix + threadNumber.incrementAndGet();
        Thread thread = new Thread(group, r, name);
        // do not set daemon
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}

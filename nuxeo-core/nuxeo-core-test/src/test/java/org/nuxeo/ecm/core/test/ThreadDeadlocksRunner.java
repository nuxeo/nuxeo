package org.nuxeo.ecm.core.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.management.jvm.ThreadDeadlocksDetector;
import org.nuxeo.runtime.management.jvm.ThreadDeadlocksDetector.KillListener;

public class ThreadDeadlocksRunner {

    protected Log log = LogFactory.getLog(ThreadDeadlocksRunner.class);

    protected Object mgr = new Object();

    protected Object repo = new Object();

    protected boolean isMgrOwned = false;

    protected boolean isRepoOwned = false;

    public  void run() {
        Thread t = new Thread() {
            public void run() {
                try {
                    synchronized(ThreadDeadlocksRunner.this) {
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

        detector.schedule(10*1000, new ThreadDeadlocksDetector.KillListener());

        new ThreadDeadlocksRunner().run();
    }
}

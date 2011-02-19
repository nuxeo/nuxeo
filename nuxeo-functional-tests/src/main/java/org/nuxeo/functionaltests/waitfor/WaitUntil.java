package org.nuxeo.functionaltests.waitfor;

public abstract class WaitUntil {

    long timeout;

    public WaitUntil(long timeout) {
        this.timeout = timeout;
    }

    public WaitUntil() {
        // default value 2 secs
        timeout = 2000;
    }

    public void waitUntil() {
        long starttime = System.currentTimeMillis();
        Exception lastException = null;

        while (starttime > System.currentTimeMillis() - timeout) {
            try {
                if (condition()) {
                    return;
                }
                Thread.sleep(100);
                lastException = null;
            } catch (Exception e) {
                lastException = e;
            }

        }
        throw new RuntimeException("Couldn't find element", lastException);
    }

    public abstract boolean condition();
}

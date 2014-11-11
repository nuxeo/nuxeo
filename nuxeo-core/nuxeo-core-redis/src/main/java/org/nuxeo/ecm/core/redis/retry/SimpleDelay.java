package org.nuxeo.ecm.core.redis.retry;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.LogFactory;

public class SimpleDelay implements Retry.Policy {

    protected final int base;

    protected final long delay;

    protected int elapsed;

    public SimpleDelay(int base, int delay) {
        this.base = base;
        this.delay = TimeUnit.MILLISECONDS.convert(delay, TimeUnit.SECONDS);
    }

    @Override
    public boolean allow() {
        return elapsed < delay;
    }

    @Override
    public void pause() {
        long computed = computeDelay();
        LogFactory.getLog(SimpleDelay.class).warn("pausing for " + computed + " ms");
        try {
            Thread.sleep(computed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        elapsed += computed;
    }

    protected long computeDelay() {
        return base;
    }

}

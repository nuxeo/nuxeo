package org.nuxeo.ecm.core.redis.retry;

import java.util.concurrent.TimeUnit;

public class ExponentialBackofDelay extends SimpleDelay {

    protected int attempt;

    public ExponentialBackofDelay(int base, int delay) {
        super(base, delay);
    }

    @Override
    protected long computeDelay() {
        int delay =  base * (1 << ++attempt);
        return TimeUnit.MILLISECONDS.convert(delay, TimeUnit.SECONDS);
    }

}

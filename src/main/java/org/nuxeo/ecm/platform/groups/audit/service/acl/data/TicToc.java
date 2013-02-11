package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

public class TicToc {
    public TicToc() {
        start = 0;
        stop = 0;
    }

    public void tic() {
        start = System.nanoTime();
    }

    /** return time in second */
    public double toc() {
        stop = System.nanoTime();
        return elapsedSecond();
    }

    public long rawToc() {
        stop = System.nanoTime();
        return stop;
    }

    public long elapsedNanosecond() {
        return stop - start;
    }

    public double elapsedMicrosecond() {
        return elapsedNanosecond() / 1000;
    }

    public double elapsedMilisecond() {
        return elapsedMicrosecond() / 1000;
    }

    public double elapsedSecond() {
        return elapsedMilisecond() / 1000;
    }

    public long getStart() {
        return start;
    }

    public long getStop() {
        return stop;
    }

    protected long start;

    protected long stop;
}
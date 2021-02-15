/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.util;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.api.Framework;

/**
 * Usage:
 *
 * <pre>
 * <code>
 * Watch w = new Watch()
 * w.start();
 * ...
 * w.start("interval-name")
 * w.stop("interval-name")
 * ..
 * w.stop()
 * </code>
 * </pre>
 *
 * @author bogdan
 * @since 9.2
 */
public class Watch {

    private static final Logger log = LogManager.getLogger(Watch.class);

    public final String label;

    public final String thresholdKey;

    public final long threshold;

    public final long intervalThreshold;

    public final TimeInterval total;

    public final Map<String, TimeInterval> intervals;

    public Watch() {
        this(null, null, null);
    }

    public Watch(String label, String thresholdKey) {
        this(label, thresholdKey, null);
    }

    public Watch(Map<String, TimeInterval> intervals) {
        this(null, null, intervals);
    }

    protected Watch(String label, String thresholdKey, Map<String, TimeInterval> intervals) {
        this.label = label;
        this.thresholdKey = thresholdKey;
        this.threshold = Long.valueOf(Framework.getProperty(thresholdKey + ".threshold", "-1"));
        this.intervalThreshold = Long.valueOf(Framework.getProperty(thresholdKey + ".intervalthreshold", "-1"));
        this.total = new TimeInterval("total");
        this.intervals = new LinkedHashMap<>(Objects.requireNonNullElse(intervals, Collections.emptyMap()));
    }

    public Watch start() {
        // reset if needed
        total.t0 = 0;
        total.t1 = 0;
        intervals.clear();

        total.start();
        return this;
    }

    public Watch stop() {
        total.stop();
        return this;
    }

    public Watch start(String interval) {
        intervals.computeIfAbsent(interval, TimeInterval::new).start();
        return this;
    }

    public Watch stop(String interval) {
        TimeInterval ti = intervals.get(interval);
        if (ti != null) {
            ti.stop();
        }
        return this;
    }

    public long elapsed(TimeUnit unit) {
        return total.elapsed(unit);
    }

    public long elapsed(String name, TimeUnit unit) {
        TimeInterval ti = intervals.get(name);
        if (ti != null) {
            return ti.elapsed(unit);
        }
        return 0;
    }

    public TimeInterval getTotal() {
        return total;
    }

    public TimeInterval[] getIntervals() {
        return intervals.values().toArray(new TimeInterval[intervals.size()]);
    }

    public void log() {
        log(TimeUnit.MILLISECONDS, threshold, intervalThreshold);
    }

    public void log(TimeUnit unit, long threshold, long intervalThreshold) {
        // use ERROR for testing
        log(Level.ERROR, unit, threshold, intervalThreshold);
    }

    public void log(Level level, TimeUnit unit, long threshold, long intervalThreshold) {
        boolean loggedTotal = false;
        if (threshold <= 0 || elapsed(unit) > threshold) {
            logTotal(level, unit, threshold, intervalThreshold);
            loggedTotal = true;
        }
        if (intervalThreshold > 0) {
            for (TimeInterval i : getIntervals()) {
                if (i.elapsed(unit) > intervalThreshold) {
                    if (!loggedTotal) {
                        logTotal(level, unit, threshold, intervalThreshold);
                        loggedTotal = true;
                    }
                    String msg = String.format("%s- %s: [%s] %s", label != null ? label + ": " : "", i.name,
                            i.elapsed(unit), unit);
                    log.log(level, msg);
                }
            }
        }
    }

    protected final void logTotal(Level level, TimeUnit unit, long threshold, long intervalThreshold) {
        String msg = String.format("%stotal: [%s] %s (threshold: %s, interval threshold: %s)",
                label != null ? label + ": " : "", elapsed(unit), unit, threshold, intervalThreshold);
        log.log(level, msg);
    }

    public static class TimeInterval implements Comparable<TimeInterval> {

        protected final String name;

        protected long t0;

        protected long t1;

        public TimeInterval(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /**
         * Elapsed time in nano seconds
         */
        public long elapsed() {
            return t1 - t0;
        }

        public long elapsed(TimeUnit unit) {
            return unit.convert(t1 - t0, TimeUnit.NANOSECONDS);
        }

        protected void start() {
            this.t0 = System.nanoTime();
        }

        protected void stop() {
            this.t1 = System.nanoTime();
        }

        public boolean isStopped() {
            return t1 != 0;
        }

        @Override
        public int compareTo(TimeInterval o) {
            long dt = (t1 - t0) - (o.t1 - o.t0); // this may be out of range for an int
            return dt < 0 ? -1 : (dt > 0 ? 1 : 0);
        }

        public String formatSeconds() {
            return new DecimalFormat("0.000").format(((double) this.t1 - this.t0) / 1000000000);
        }

        @Override
        public String toString() {
            return name + ": " + this.formatSeconds() + " sec.";
        }

    }

}

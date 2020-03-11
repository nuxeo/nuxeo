/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.log.chronicle;

import static net.openhft.chronicle.queue.RollCycles.DAILY;
import static net.openhft.chronicle.queue.RollCycles.HOURLY;
import static net.openhft.chronicle.queue.RollCycles.MINUTELY;
import static net.openhft.chronicle.queue.RollCycles.TEST_SECONDLY;

import net.openhft.chronicle.queue.RollCycle;

/**
 * @since 9.3
 */
public class ChronicleRetentionDuration {

    protected static final String SECOND_ROLLING_PERIOD = "s";

    protected static final String MINUTE_ROLLING_PERIOD = "m";

    protected static final String HOUR_ROLLING_PERIOD = "h";

    protected static final String DAY_ROLLING_PERIOD = "d";

    protected final RollCycle rollCycle;

    protected final int retentionCycles;

    protected final String retention;

    public static final ChronicleRetentionDuration NONE = new ChronicleRetentionDuration("0d");

    public ChronicleRetentionDuration(String retention) {
        this.retention = decodeRetention(retention);
        this.rollCycle = decodeRollCycle(this.retention);
        this.retentionCycles = decodeRetentionCycles(this.retention);
    }

    protected String decodeRetention(String retention) {
        if (retention == null || retention.isEmpty()) {
            return "0d";
        }
        return retention;
    }

    @Override
    public String toString() {
        return retention;
    }

    public boolean disable() {
        return retentionCycles <= 0;
    }

    public RollCycle getRollCycle() {
        return rollCycle;
    }

    @SuppressWarnings("unused")
    public String getRetention() {
        return retention;
    }

    public int getRetentionCycles() {
        return retentionCycles;
    }

    protected RollCycle decodeRollCycle(String retentionDuration) {
        if (retentionDuration == null || retentionDuration.isEmpty()) {
            return DAILY;
        }
        String rollingPeriod = retentionDuration.substring(retentionDuration.length() - 1);
        switch (rollingPeriod) {
        case SECOND_ROLLING_PERIOD:
            return TEST_SECONDLY;
        case MINUTE_ROLLING_PERIOD:
            return MINUTELY;
        case HOUR_ROLLING_PERIOD:
            return HOURLY;
        case DAY_ROLLING_PERIOD:
            return DAILY;
        default:
            throw new IllegalArgumentException("Unknown rolling period: " + rollingPeriod);
        }
    }

    protected int decodeRetentionCycles(String retentionDuration) {
        if (retentionDuration != null) {
            return Integer.parseInt(retentionDuration.substring(0, retentionDuration.length() - 1));
        }
        return 0;
    }

    protected static String encodeRollCycle(RollCycle rollCycle) {
        if (rollCycle.equals(TEST_SECONDLY)) {
            return SECOND_ROLLING_PERIOD;
        }
        if (rollCycle.equals(MINUTELY)) {
            return MINUTE_ROLLING_PERIOD;
        }
        if (rollCycle.equals(HOURLY)) {
            return HOUR_ROLLING_PERIOD;
        }
        if (rollCycle.equals(DAILY)) {
            return DAY_ROLLING_PERIOD;
        }
        throw new IllegalArgumentException("Unknown rolling cycle: " + rollCycle);
    }

    public static ChronicleRetentionDuration disableOf(ChronicleRetentionDuration retention) {
        return new ChronicleRetentionDuration("0" + encodeRollCycle(retention.getRollCycle()));
    }
}

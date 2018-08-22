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

import net.openhft.chronicle.queue.RollCycle;
import net.openhft.chronicle.queue.RollCycles;

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
        this.retention = retention;
        this.rollCycle = decodeRollCycle(retention);
        this.retentionCycles = decodeRetentionCycles(retention);
    }

    @Override
    public String toString() {
        return disable() ? "disabled" : retention;
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
            return RollCycles.DAILY;
        }
        String rollingPeriod = retentionDuration.substring(retentionDuration.length() - 1);
        switch (rollingPeriod) {
        case SECOND_ROLLING_PERIOD:
            return RollCycles.TEST_SECONDLY;
        case MINUTE_ROLLING_PERIOD:
            return RollCycles.MINUTELY;
        case HOUR_ROLLING_PERIOD:
            return RollCycles.HOURLY;
        case DAY_ROLLING_PERIOD:
            return RollCycles.DAILY;
        default:
            String msg = "Unknown rolling period: " + rollingPeriod;
            throw new IllegalArgumentException(msg);
        }
    }

    protected int decodeRetentionCycles(String retentionDuration) {
        if (retentionDuration != null) {
            return Integer.parseInt(retentionDuration.substring(0, retentionDuration.length() - 1));
        }
        return 0;
    }

}

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
package org.nuxeo.lib.stream.computation;

import java.util.Objects;

/**
 * Watermark represents a point in time. This point in time is composed of a millisecond timestamp and a sequence. There
 * is also a state to denote if the point in time is reached (completed) or not. Watermark are immutable.
 *
 * @since 9.3
 */
public final class Watermark implements Comparable<Watermark> {
    public static final Watermark LOWEST = new Watermark(0, (short) 0, false);

    protected final long timestamp;

    protected final short sequence;

    protected final boolean completed;

    protected final long value;

    private Watermark(long timestamp, short sequence, boolean completed) {
        if (timestamp < 0) {
            throw new IllegalArgumentException("timestamp must be positive");
        }
        this.timestamp = timestamp;
        this.sequence = sequence;
        this.completed = completed;
        this.value = timestamp << 17 | (sequence & 0xFFFF) << 1 | (completed ? 1 : 0);
    }

    public static Watermark ofValue(long watermarkValue) {
        if (watermarkValue < 0) {
            throw new IllegalArgumentException("Watermark must be positive");
        }
        return new Watermark(watermarkValue >> 17, (short) ((watermarkValue >> 1) & 0xFFFF),
                (watermarkValue & 1L) == 1L);
    }

    public static Watermark ofNow() {
        return ofTimestamp(System.currentTimeMillis(), (short) 0);
    }

    public static Watermark ofTimestamp(long timestamp) {
        return ofTimestamp(timestamp, (short) 0);
    }

    public static Watermark ofTimestamp(long timestamp, short sequence) {
        return new Watermark(timestamp, sequence, false);
    }

    public static Watermark completedOf(Watermark watermark) {
        Objects.requireNonNull(watermark);
        return new Watermark(watermark.getTimestamp(), watermark.getSequence(), true);
    }

    public long getValue() {
        return value;
    }

    public boolean isCompleted() {
        return completed;
    }

    public short getSequence() {
        return sequence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isDone(long timestamp) {
        return Watermark.ofTimestamp(timestamp).compareTo(this) < 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Watermark watermark = (Watermark) o;
        return completed == watermark.completed && timestamp == watermark.timestamp && sequence == watermark.sequence;
    }

    @Override
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    @Override
    public String toString() {
        return "Watermark{" + "completed=" + completed + ", timestamp=" + timestamp + ", sequence=" + sequence
                + ", value=" + value + '}';
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Watermark o) {
        if (o == null) {
            return Integer.MAX_VALUE;
        }
        long diff = value - o.value;
        // cast diff to int when possible
        int ret = (int) diff;
        if (ret == diff) {
            return ret;
        }
        if (diff > 0) {
            return 1;
        }
        return -1;
    }

}

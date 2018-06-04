/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.log;

import static java.lang.Math.max;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extends LogLag with lower and upper timestamps to express lag as a latency.
 *
 * @since 10.1
 */
public class Latency {
    protected final LogLag lag;

    protected final long lower;

    protected final long upper;

    protected final String key;

    public Latency(long lower, long upper, LogLag lag, String key) {
        Objects.requireNonNull(lag);
        this.lower = lower;
        this.upper = upper;
        this.lag = lag;
        this.key = key;
    }

    public static Latency noLatency(long upper, LogLag lag) {
        Objects.requireNonNull(lag);
        if (lag.lag() != 0) {
            throw new IllegalArgumentException("Lag found: " + lag);
        }
        return new Latency(0, upper, lag, null);
    }

    public static Latency fromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode obj = mapper.readTree(json);
            long lower = obj.get("low").asLong();
            long upper = obj.get("up").asLong();
            long lag = obj.get("lag").asLong();
            String key = obj.get("key") == null ? null : obj.get("key").asText();
            return new Latency(lower, upper, LogLag.of(lag), key);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid json: " + json, e);
        }
    }

    public static Latency of(List<Latency> latencies) {
        LogLag lag = LogLag.of(latencies.stream().map(Latency::lag).collect(Collectors.toList()));
        final long[] start = { Long.MAX_VALUE };
        final long[] end = { 0 };
        final String[] key = { "" };
        latencies.forEach(item -> {
            if (item.lower > 0 && item.lower < start[0]) {
                start[0] = item.lower;
                key[0] = item.key;
            }
            end[0] = max(end[0], item.upper);
        });
        return new Latency(start[0] == Long.MAX_VALUE ? 0 : start[0], end[0], lag, key[0]);
    }

    /**
     * Returns the latency expressed in millisecond.
     */
    public long latency() {
        return lag.lag() > 0 ? upper - lower : 0;
    }

    /**
     * Returns the lower timestamp.
     */
    public long lower() {
        return lower;
    }

    /**
     * Returns the upper timestamp.
     */
    public long upper() {
        return upper;
    }

    public LogLag lag() {
        return lag;
    }

    /**
     * Returns the key associated with the lower timestamp.
     */
    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return "Latency{" + "lat=" + latency() + ", lower=" + lower + ", upper=" + upper + ", key=" + key + ", lag="
                + lag + '}';
    }

    public String asJson() {
        return String.format("{\"lat\":\"%s\",\"low\":\"%s\",\"up\":\"%s\",\"lag\":\"%s\"%s}", latency(), lower, upper,
                lag.lag, key == null ? "" : ",\"key\":\"" + key + "\"");
    }
}

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
package org.nuxeo.lib.stream.pattern.producer;

import java.util.List;

import org.nuxeo.lib.stream.pattern.producer.internals.ProducerRunner;

/**
 * The return status of a {@link ProducerRunner}
 *
 * @since 9.1
 */
public class ProducerStatus {
    public final long startTime;

    public final long stopTime;

    public final long nbProcessed;

    public final int producer;

    protected final boolean fail;

    public ProducerStatus(int producer, long nbProcessed, long startTime, long stopTime, boolean fail) {
        this.producer = producer;
        this.nbProcessed = nbProcessed;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.fail = fail;
    }

    @Override
    public String toString() {
        if (fail) {
            return "Producer status FAILURE";
        }
        double elapsed = (stopTime - startTime) / 1000.;
        double mps = (elapsed != 0) ? nbProcessed / elapsed : 0.0;
        return String.format("Producer %02d status: messages: %d, elapsed: %.2fs, throughput: %.2f msg/s.", producer,
                nbProcessed, elapsed, mps);
    }

    static String toString(List<ProducerStatus> stats) {
        long startTime = stats.stream().mapToLong(r -> r.startTime).min().orElse(0);
        long stopTime = stats.stream().mapToLong(r -> r.stopTime).max().orElse(0);
        double elapsed = (stopTime - startTime) / 1000.;
        long messages = stats.stream().mapToLong(r -> r.nbProcessed).sum();
        double mps = (elapsed != 0) ? messages / elapsed : 0.0;
        int producers = stats.size();
        long failures = stats.stream().filter(s -> s.fail).count();
        return String.format(
                "Producers status: threads: %d, failures: %d, messages: %d, elapsed: %.2fs, throughput: %.2f msg/s",
                producers, failures, messages, elapsed, mps);

    }
}

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

import java.time.Duration;

import org.nuxeo.lib.stream.log.Latency;

/**
 * Run a topology of computations according to some settings.
 *
 * @since 9.3
 */
public interface StreamProcessor {

    /**
     * Initialize streams, but don't run the computations
     */
    StreamProcessor init(Topology topology, Settings settings);

    /**
     * Run the initialized computations.
     */
    void start();

    /**
     * Try to stop computations gracefully after processing a record or a timer within the timeout duration. If this can
     * not be done within the timeout, shutdown and returns false.
     */
    boolean stop(Duration timeout);

    /**
     * Stop computations when input streams are empty. The timeout is applied for each computation, the total duration
     * can be up to nb computations * timeout
     * <p/>
     * Returns {@code true} if computations are stopped during the timeout delay.
     */
    boolean drainAndStop(Duration timeout);

    /**
     * Shutdown immediately.
     */
    void shutdown();

    /**
     * Returns the low watermark for the computation. Any message with an offset below the low watermark has been
     * processed by this computation and its ancestors. The returned watermark is local to this processing node, if the
     * computation is distributed the global low watermark is the minimum of all nodes low watermark.
     */
    long getLowWatermark(String computationName);

    /**
     * Returns the low watermark for all the computations of the topology. Any message with an offset below the low
     * watermark has been processed. The returned watermark is local to this processing node.
     */
    long getLowWatermark();

    /**
     * Returns the latency for a computation. This works also for distributed computations.
     *
     * @since 10.1
     */
    Latency getLatency(String computationName);

    /**
     * Returns true if all messages with a lower timestamp has been processed by the topology.
     */
    boolean isDone(long timestamp);

    /**
     * Wait for the computations to have assigned partitions ready to process records. The processor must be started.
     * This is useful for writing unit test.
     * <p/>
     * Returns {@code true} if all computations have assigned partitions during the timeout delay.
     */
    boolean waitForAssignments(Duration timeout) throws InterruptedException;

    /**
     * True if there is no active processing threads.
     *
     * @since 10.1
     */
    boolean isTerminated();
}

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
 *     Adapted from https://github.com/concord/concord-jvm
 *     bdelbosc
 */
package org.nuxeo.lib.stream.computation;

/**
 * Computation receives records from input streams one at a time, it can produce record on its output streams. A timer
 * processing can be used for windowing computation.
 *
 * @since 9.3
 */
public interface Computation {

    /**
     * Called when the framework has registered the computation successfully. Gives users a first opportunity to
     * schedule timer callbacks and produce records. This method can be called multiple times.
     *
     * @param context The computation context object provided by the system.
     */
    void init(ComputationContext context);

    /**
     * Called when the framework is ready to shutdown the computation. Gives users a chance to perform some cleanup
     * before the process is killed.
     */
    @SuppressWarnings("EmptyMethod")
    default void destroy() {

    }

    /**
     * Process an incoming record on one of the computation's input streams.
     *
     * @param context The computation context object provided by the system.
     * @param inputStreamName Name of the input stream that provides the record.
     * @param record The record.
     */
    void processRecord(ComputationContext context, String inputStreamName, Record record);

    /**
     * Process a timer callback previously set via {@link ComputationContext#setTimer(String, long)}.
     *
     * @param context The computation context object provided by the system.
     * @param key The name of the timer.
     * @param timestamp The timestamp (in ms) for which the callback was scheduled.
     */
    void processTimer(ComputationContext context, String key, long timestamp);

    /**
     * Identify the computation.
     *
     * @return computation's metadata.
     */
    ComputationMetadata metadata();

    /**
     * A hook to inform that computation will be soon destroyed. It gives a way for long processing to cooperate to a
     * quick shutdown. <br>
     * This method is not invoked from the computation thread, it should only set some volatile flag and returns
     * immediately.
     *
     * @since 10.2
     */
    @SuppressWarnings("EmptyMethod")
    default void signalStop() {
    }

    /**
     * Called after a failure in {@link #processRecord} or {@link #processTimer} before retrying.
     *
     * @since 10.3
     */
    void processRetry(ComputationContext context, Throwable failure);

    /**
     * Called when {@link #processRecord} or {@link #processTimer} fails and cannot be retried.
     *
     * @since 10.3
     */
    void processFailure(ComputationContext context, Throwable failure);

}

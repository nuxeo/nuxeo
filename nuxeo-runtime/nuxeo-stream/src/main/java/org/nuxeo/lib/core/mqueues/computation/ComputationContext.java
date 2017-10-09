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
 *     Adapted from from https://github.com/concord/concord-jvm
 *     bdelbosc
 */
package org.nuxeo.lib.core.mqueues.computation;

/**
 * @since 9.2
 */
public interface ComputationContext {

    /**
     * Set local state for a given key.
     *
     * @param key Key to set in local store.
     * @param binaryValue Value to store at key.
     */
    void setState(final String key, final byte[] binaryValue);

    /**
     * Get local state for a given key
     *
     * @param key Key to receive from local store.
     * @return the state executed upon data retrieval.
     */
    byte[] getState(final String key);

    /**
     * Register a timer callback for some point in the future
     *
     * @param key Name of the timer callback.
     * @param time  The (ms since epoch) at which the callback should be fired
     */
    void setTimer(final String key, final long time);

    /**
     * Emit a record downstream. Records are send effectively on checkpoint using {@link #askForCheckpoint()}.
     *
     * @param streamName The name of the stream on which the record should be emitted.
     * @param key The key associated with the record. Only relevant when routing method is `GROUP_BY`.
     * @param data: The binary blob to send downstream.
     */
    void produceRecord(final String streamName, final String key, final byte[] data);

    void produceRecord(final String streamName, final Record record);

    /**
     * Set the low watermark for a source computation.
     */
    void setSourceLowWatermark(long watermark);

    /**
     * Ask for checkpoint in order to send records, save input stream offset positions.
     */
    void askForCheckpoint();
}
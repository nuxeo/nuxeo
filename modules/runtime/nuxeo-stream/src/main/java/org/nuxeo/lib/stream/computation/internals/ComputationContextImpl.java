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
package org.nuxeo.lib.stream.computation.internals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.log.LogOffset;

/**
 * @since 9.3
 */
public class ComputationContextImpl implements ComputationContext {
    protected final ComputationMetadataMapping metadata;

    protected final Map<String, List<Record>> streamRecords;

    protected final Map<String, Long> timers;

    protected final StreamManager manager;

    protected final ComputationPolicy policy;

    protected final boolean isSpare;

    protected boolean checkpointFlag;

    protected long lowWatermark;

    protected boolean terminateFlag;

    protected LogOffset lastOffset;

    public ComputationContextImpl(StreamManager streamManager, ComputationMetadataMapping metadata,
            ComputationPolicy policy, boolean isSpare) {
        this.manager = streamManager;
        this.metadata = metadata;
        this.timers = new HashMap<>();
        this.streamRecords = new HashMap<>();
        this.policy = policy;
        this.isSpare = isSpare;
    }

    public ComputationContextImpl(StreamManager streamManager, ComputationMetadataMapping metadata,
            ComputationPolicy policy) {
        this(streamManager, metadata, policy, false);
    }

    public ComputationContextImpl(ComputationMetadataMapping computationMetadataMapping) {
        this(null, computationMetadataMapping, ComputationPolicy.NONE, false);
    }

    public List<Record> getRecords(String streamName) {
        return streamRecords.getOrDefault(streamName, Collections.emptyList());
    }

    public Map<String, Long> getTimers() {
        return timers;
    }

    @Override
    public void setTimer(String key, long time) {
        Objects.requireNonNull(key);
        timers.put(key, time);
    }

    public void removeTimer(String key) {
        Objects.requireNonNull(key);
        timers.remove(key);
    }

    @Override
    public void produceRecord(String streamName, Record record) {
        String targetStream = metadata.map(streamName);
        if (!metadata.outputStreams().contains(targetStream)) {
            throw new IllegalArgumentException("Stream not registered as output: " + targetStream + ":" + streamName);
        }
        streamRecords.computeIfAbsent(targetStream, key -> new ArrayList<>()).add(record);
    }

    /**
     * Writes to an output stream immediately. This will creates systematically duplicates on errors, always use
     * {@link #produceRecord(String, Record)} when possible.
     */
    public LogOffset produceRecordImmediate(String streamName, Record record) {
        if (manager == null) {
            throw new IllegalStateException("No logManager provided in context");
        }
        String targetStream = metadata.map(streamName);
        if (!metadata.outputStreams().contains(targetStream)) {
            throw new IllegalArgumentException("Stream not registered as output: " + targetStream + ":" + streamName);
        }
        return manager.append(targetStream, record);
    }

    public void produceRecordImmediate(String streamName, String key, byte[] data) {
        produceRecordImmediate(streamName, Record.of(key, data));
    }

    @Override
    public LogOffset getLastOffset() {
        return lastOffset;
    }

    @Override
    public ComputationPolicy getPolicy() {
        return policy;
    }

    @Override
    public boolean isSpareComputation() {
        return isSpare;
    }

    public void setLastOffset(LogOffset lastOffset) {
        this.lastOffset = lastOffset;
    }

    public long getSourceLowWatermark() {
        return lowWatermark;
    }

    @Override
    public void setSourceLowWatermark(long watermark) {
        this.lowWatermark = watermark;
    }

    public boolean requireCheckpoint() {
        return checkpointFlag;
    }

    public void removeCheckpointFlag() {
        checkpointFlag = false;
    }

    @Override
    public void askForCheckpoint() {
        checkpointFlag = true;
    }

    @Override
    public void cancelAskForCheckpoint() {
        checkpointFlag = false;
    }

    @Override
    public void askForTermination() {
        terminateFlag = true;
    }

    public boolean requireTerminate() {
        return terminateFlag;
    }

    @Override
    public String toString() {
        return "ComputationContextImpl{" +
                "metadata=" + metadata +
                ", streamRecords=" + streamRecords +
                ", timers=" + timers +
                ", manager=" + manager +
                ", policy=" + policy +
                ", isSpare=" + isSpare +
                ", checkpointFlag=" + checkpointFlag +
                ", lowWatermark=" + lowWatermark +
                ", terminateFlag=" + terminateFlag +
                ", lastOffset=" + lastOffset +
                '}';
    }
}

/*
 * (C) Copyright 2022 Nuxeo.
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
 */
package org.nuxeo.lib.stream.log.mem;

import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.io.Externalizable;
import java.time.Duration;
import java.util.Objects;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.internals.CloseableLogAppender;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;
import org.nuxeo.lib.stream.log.mem.MemLogPartition.OffsetTracker;

/**
 * Memory implementation of LogAppender.
 */
public class MemLogAppender<M extends Externalizable> implements CloseableLogAppender<M> {

    private static final Logger log = LogManager.getLogger(MemLogAppender.class);

    protected static final String NOCODEC_TAG = "nocodec";

    private final Name name;

    private final Codec<M> codec;

    private final MemLog memLog;

    private volatile boolean closed;

    public MemLogAppender(MemLogs memLogs, Name name, Codec<M> codec) {
        Objects.requireNonNull(codec);
        this.name = name;
        this.codec = codec;

        memLog = memLogs.getLog(name);
        log.debug("Opening: {}", this);
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public Codec<M> getCodec() {
        return codec;
    }

    @Override
    public boolean closed() {
        return closed;
    }

    @Override
    public int size() {
        return memLog.size();
    }

    @Override
    public LogOffset append(int partition, M message) {
        if (closed) {
            throw new IndexOutOfBoundsException();
        }
        String tag = NO_CODEC.equals(codec) ? NOCODEC_TAG : null;
        byte[] bytes;
        if (tag != null) {
            // default format for backward compatibility
            try {
                bytes = SerializationUtils.serialize(message);
            } catch (SerializationException e) {
                throw new StreamRuntimeException(e);
            }
        } else {
            bytes = codec.encode(message);
        }
        long offset = memLog.getPartition(partition).append(tag, bytes);
        LogOffset ret = new LogOffsetImpl(name, partition, offset);
        log.debug("append to {}, value: {}", ret, message);
        return ret;
    }

    public LogTailer<M> createTailer(LogPartition partition, Name group, Codec<M> codec) {
        return new MemLogTailer<>(memLog, partition, group, codec);
    }

    @Override
    public boolean waitFor(LogOffset logOffset, Name group, Duration timeout) throws InterruptedException {
        long offset = logOffset.offset();
        OffsetTracker offsetTracker = memLog.getPartition(logOffset.partition().partition())
                                            .getCommittedOffsetTracker(group);
        long remaining = timeout.toNanos();
        long deadline = System.nanoTime() + remaining;
        boolean processed;
        while (!(processed = isProcessed(offsetTracker, offset)) && remaining > 0) {
            offsetTracker.awaitNanos(remaining);
            remaining = deadline - System.nanoTime();
        }
        return processed;
    }

    private boolean isProcessed(OffsetTracker offsetTracker, long offset) {
        long committed = offsetTracker.get();
        return committed > 0 && committed >= offset;
    }

    @Override
    public void close() {
        log.debug("Closing: {}", this);
        closed = true;
    }

    @Override
    public String toString() {
        return "MemLogAppender{nbPartitions=%d, name='%s', closed=%s, codec=%s}".formatted(memLog.size(), name, closed,
                codec);
    }

}

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

import static net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder.binary;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.log.Name;

import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.queue.TailerState;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

/**
 * Track committed offset for a Log.
 *
 * @since 9.3
 */
public class ChronicleLogOffsetTracker implements AutoCloseable {
    private static final Log log = LogFactory.getLog(ChronicleLogOffsetTracker.class);

    protected static final String OFFSET_QUEUE_PREFIX = "offset-";

    // message are small, minimum block size of 256K is buggy so take the size above
    // this will create a cq4 file of 1.3MB max message size is around 256KB
    public static final int CQ_BLOCK_SIZE = 1_048_576;

    protected final SingleChronicleQueue offsetQueue;

    protected final int partition;

    protected long lastCommittedOffset;

    protected final ChronicleRetentionDuration retention;

    public ChronicleLogOffsetTracker(String basePath, int partition, Name group,
            ChronicleRetentionDuration retention) {
        this.partition = partition;
        this.retention = retention;
        File offsetFile = new File(basePath, OFFSET_QUEUE_PREFIX + group.getId());
        ChronicleRetentionListener listener = null;
        SingleChronicleQueueBuilder builder = binary(offsetFile).rollCycle(retention.getRollCycle())
                                                                .blockSize(CQ_BLOCK_SIZE);
        if (!retention.disable() && partition == 0) {
            // offset queue is shared among partitions
            // only the first partition handle the retention
            listener = new ChronicleRetentionListener(retention);
            builder.storeFileListener(listener);

        }
        offsetQueue = builder.build();
        if (listener != null) {
            listener.setQueue(offsetQueue);
        }
    }

    public static boolean exists(Path basePath, Name group) {
        try (Stream<Path> paths = Files.list(basePath.resolve(OFFSET_QUEUE_PREFIX + group.getId()))) {
            return paths.count() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isOffsetTracker(String dirName) {
        return dirName.startsWith(OFFSET_QUEUE_PREFIX);
    }

    public static String getGroupFromDirectory(String dirName) {
        if (!isOffsetTracker(dirName)) {
            throw new IllegalArgumentException(String.format("Invalid directory %s, not an offset tracker", dirName));
        }
        return dirName.replaceFirst(OFFSET_QUEUE_PREFIX, "");
    }

    /**
     * Use a cache to return the last committed offset, concurrent consumer is not taken in account use
     * {@link #readLastCommittedOffset()} in concurrency.
     */
    public long getLastCommittedOffset() {
        if (lastCommittedOffset > 0) {
            return lastCommittedOffset;
        }
        return readLastCommittedOffset();
    }

    /**
     * Read the last committed offset from the file.
     */
    public long readLastCommittedOffset() {
        ExcerptTailer offsetTailer;
        try {
            offsetTailer = offsetQueue.createTailer().direction(TailerDirection.BACKWARD).toEnd();
        } catch (IllegalStateException e) {
            // sometime the end is NOT_REACHED, may be because the queue is not yet fully initialized
            log.warn(String.format("Fail to reach the end of offset queue: %s because of: %s, retrying.",
                    offsetQueue.file().getAbsolutePath(), e.getMessage()));
            offsetTailer = offsetQueue.createTailer().direction(TailerDirection.BACKWARD).toEnd();
        }
        if (offsetTailer.state() == TailerState.UNINITIALISED) {
            // This is a new queue, we are not going to find anything
            return 0;
        }
        final long[] offset = { 0 };
        boolean hasNext;
        do {
            hasNext = offsetTailer.readBytes(b -> {
                int queue = b.readInt();
                long off = b.readLong();
                b.readLong(); // stamp not used
                if (partition == queue) {
                    offset[0] = off;
                }
            });
        } while (offset[0] == 0 && hasNext);
        return offset[0];
    }

    public void commit(long offset) {
        ExcerptAppender appender = offsetQueue.acquireAppender();
        appender.writeBytes(b -> b.writeInt(partition).writeLong(offset).writeLong(System.currentTimeMillis()));
        lastCommittedOffset = offset;
    }

    @Override
    public void close() {
        if (!offsetQueue.isClosed()) {
            offsetQueue.close();
        }
    }

}

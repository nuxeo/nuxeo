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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;

/**
 * Track committed offset for a queue.
 *
 * @since 9.3
 */
public class ChronicleLogOffsetTracker implements AutoCloseable {
    protected static final String OFFSET_QUEUE_PREFIX = "offset-";

    private static final Log log = LogFactory.getLog(ChronicleLogOffsetTracker.class);

    protected final SingleChronicleQueue offsetQueue;

    protected final int partition;

    protected long lastCommittedOffset;

    public ChronicleLogOffsetTracker(String basePath, int partition, String group) {
        this.partition = partition;
        File offsetFile = new File(basePath, OFFSET_QUEUE_PREFIX + group);
        offsetQueue = binary(offsetFile).build();
        offsetQueue.acquireAppender().pretouch();
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
        final long[] offset = { 0 };
        boolean hasNext;
        do {
            hasNext = offsetTailer.readBytes(b -> {
                int queue = b.readInt();
                long off = b.readLong();
                long stamp = b.readLong();
                if (partition == queue) {
                    offset[0] = off;
                }
            });
        } while (offset[0] == 0 && hasNext);
        // System.out.println("last committed returned from: " + offsetQueue.file() + " " + offset[0] + " after reading
        // " + count[0]);
        return offset[0];
    }

    public void commit(long offset) {
        ExcerptAppender appender = offsetQueue.acquireAppender();
        appender.writeBytes(b -> b.writeInt(partition).writeLong(offset).writeLong(System.currentTimeMillis()));
        // System.out.println(String.format("COMMIT %s, partition: %s, offset: %s, pos: %s",
        // offsetQueue.file(), partition, offset, appender.lastIndexAppended()));
        lastCommittedOffset = offset;
    }

    @Override
    public void close() {
        offsetQueue.close();
    }

}

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

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.internals.CloseableLogAppender;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

/**
 * Chronicle Queue implementation of LogAppender.
 *
 * @since 9.3
 */
public class ChronicleLogAppender<M extends Externalizable> implements CloseableLogAppender<M> {
    private static final Log log = LogFactory.getLog(ChronicleLogAppender.class);

    protected static final String PARTITION_PREFIX = "P-";

    protected static final int POLL_INTERVAL_MS = 100;

    protected static final int MAX_PARTITIONS = 100;

    public static final String MSG_KEY = "msg";

    protected final List<ChronicleQueue> partitions;

    protected final int nbPartitions;

    protected final File basePath;

    protected final String name;

    // keep track of created tailers to make sure they are closed before the log
    protected final ConcurrentLinkedQueue<ChronicleLogTailer<M>> tailers = new ConcurrentLinkedQueue<>();

    protected final ChronicleRetentionDuration retention;

    protected final Codec<M> codec;

    protected volatile boolean closed;

    protected ChronicleLogAppender(Codec<M> codec, File basePath, int size, ChronicleRetentionDuration retention) {
        if (size == 0) {
            // open
            if (!exists(basePath)) {
                throw new IllegalArgumentException("Cannot open Chronicle Queues, invalid path: " + basePath);
            }
            this.nbPartitions = partitions(basePath);
        } else {
            // create
            if (size > MAX_PARTITIONS) {
                throw new IllegalArgumentException(
                        String.format("Cannot create more than: %d partitions for log: %s, requested: %d",
                                MAX_PARTITIONS, basePath, size));
            }
            if (exists(basePath)) {
                throw new IllegalArgumentException("Cannot create Chronicle Queues, already exists: " + basePath);
            }
            if (!basePath.exists() && !basePath.mkdirs()) {
                throw new IllegalArgumentException("Invalid path to create Chronicle Queues: " + basePath);
            }
            this.nbPartitions = size;
        }
        Objects.requireNonNull(codec);
        this.codec = codec;
        this.name = basePath.getName();
        this.basePath = basePath;
        this.retention = retention;
        partitions = new ArrayList<>(this.nbPartitions);
        if (log.isDebugEnabled()) {
            log.debug(((size == 0) ? "Opening: " : "Creating: ") + toString());
        }
        initPartitions();
    }

    protected void initPartitions() {
        for (int i = 0; i < nbPartitions; i++) {
            File path = new File(basePath, String.format("%s%02d", PARTITION_PREFIX, i));
            if (retention.disable()) {
                partitions.add(SingleChronicleQueueBuilder.binary(path).build());
            } else {
                ChronicleRetentionListener listener = new ChronicleRetentionListener(retention);
                SingleChronicleQueue queue = SingleChronicleQueueBuilder.binary(path)
                                                                        .rollCycle(retention.getRollCycle())
                                                                        .storeFileListener(listener)
                                                                        .build();
                listener.setQueue(queue);
                partitions.add(queue);
            }
            try {
                // make sure the directory is created so we can count the partitions
                Files.createDirectories(path.toPath());
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot create directory: " + path.getAbsolutePath(), e);
            }
        }
    }

    protected static boolean exists(File basePath) {
        // noinspection ConstantConditions
        return basePath.isDirectory() && basePath.list().length > 0;
    }

    /**
     * Create a new log
     */
    public static <M extends Externalizable> ChronicleLogAppender<M> create(Codec<M> codec, File basePath, int size,
            ChronicleRetentionDuration retention) {
        return new ChronicleLogAppender<>(codec, basePath, size, retention);
    }

    /**
     * Create a new log.
     */
    public static <M extends Externalizable> ChronicleLogAppender<M> create(Codec<M> codec, File basePath, int size) {
        return new ChronicleLogAppender<>(codec, basePath, size, ChronicleRetentionDuration.DISABLE);
    }

    /**
     * Open an existing log.
     */
    public static <M extends Externalizable> ChronicleLogAppender<M> open(Codec<M> codec, File basePath) {
        return new ChronicleLogAppender<>(codec, basePath, 0, ChronicleRetentionDuration.DISABLE);
    }

    /**
     * Open an existing log.
     */
    public static <M extends Externalizable> ChronicleLogAppender<M> open(Codec<M> codec, File basePath,
            ChronicleRetentionDuration retention) {
        return new ChronicleLogAppender<>(codec, basePath, 0, retention);
    }

    public String getBasePath() {
        return basePath.getPath();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int size() {
        return nbPartitions;
    }

    @Override
    public LogOffset append(int partition, M message) {
        ExcerptAppender appender = partitions.get(partition).acquireAppender();
        if (NO_CODEC.equals(codec)) {
            // default format for backward compatibility
            appender.writeDocument(w -> w.write(MSG_KEY).object(message));
        } else {
            appender.writeDocument(w -> w.write().bytes(codec.encode(message)));
        }
        long offset = appender.lastIndexAppended();
        LogOffset ret = new LogOffsetImpl(name, partition, offset);
        if (log.isDebugEnabled()) {
            log.debug(String.format("append to %s, value: %s", ret, message));
        }
        return ret;
    }

    public LogTailer<M> createTailer(LogPartition partition, String group, Codec<M> codec) {
        return addTailer(new ChronicleLogTailer<>(codec, basePath.toString(),
                partitions.get(partition.partition()).createTailer(), partition, group, retention));
    }

    public long endOffset(int partition) {
        return partitions.get(partition).createTailer().toEnd().index();
    }

    public long firstOffset(int partition) {
        long ret = partitions.get(partition).firstIndex();
        if (ret == Long.MAX_VALUE) {
            return 0;
        }
        return ret;
    }

    public long countMessages(int partition, long lowerOffset, long upperOffset) {
        long ret;
        SingleChronicleQueue queue = (SingleChronicleQueue) partitions.get(partition);
        try {
            ret = queue.countExcerpts(lowerOffset, upperOffset);
        } catch (IllegalStateException e) {
            if (log.isDebugEnabled()) {
                log.debug("Missing low cycle file: " + lowerOffset + " for queue: " + queue + " " + e.getMessage());
            }
            return 0;
        }
        return ret;
    }

    protected LogTailer<M> addTailer(ChronicleLogTailer<M> tailer) {
        tailers.add(tailer);
        return tailer;
    }

    @Override
    public boolean waitFor(LogOffset offset, String group, Duration timeout) throws InterruptedException {
        boolean ret;
        long offsetPosition = offset.offset();
        int partition = offset.partition().partition();
        try (ChronicleLogOffsetTracker offsetTracker = new ChronicleLogOffsetTracker(basePath.toString(), partition,
                group)) {
            ret = isProcessed(offsetTracker, offsetPosition);
            if (ret) {
                return true;
            }
            final long timeoutMs = timeout.toMillis();
            final long deadline = System.currentTimeMillis() + timeoutMs;
            final long delay = Math.min(POLL_INTERVAL_MS, timeoutMs);
            while (!ret && System.currentTimeMillis() < deadline) {
                Thread.sleep(delay);
                ret = isProcessed(offsetTracker, offsetPosition);
            }
        }
        return ret;
    }

    @Override
    public boolean closed() {
        return closed;
    }

    @Override
    public Codec<M> getCodec() {
        return codec;
    }

    protected boolean isProcessed(ChronicleLogOffsetTracker tracker, long offset) {
        long last = tracker.readLastCommittedOffset();
        return last > 0 && last >= offset;
    }

    @Override
    public void close() {
        log.debug("Closing: " + toString());
        tailers.stream().filter(Objects::nonNull).forEach(ChronicleLogTailer::close);
        tailers.clear();
        partitions.stream().filter(Objects::nonNull).forEach(ChronicleQueue::close);
        partitions.clear();
        closed = true;
    }

    public static int partitions(File basePath) {
        int ret;
        try (Stream<Path> paths = Files.list(basePath.toPath())) {
            ret = (int) paths.filter(
                    path -> (path.toFile().isDirectory() && path.getFileName().toString().startsWith(PARTITION_PREFIX)))
                             .count();
            if (ret == 0) {
                throw new IOException("No chronicles queues file found");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid basePath for queue: " + basePath, e);
        }
        return ret;
    }

    @Override
    public String toString() {
        return "ChronicleLogAppender{" + "nbPartitions=" + nbPartitions + ", basePath=" + basePath + ", name='" + name
                + '\'' + ", retention=" + retention + ", closed=" + closed + ", codec=" + codec + '}';
    }

    public ChronicleRetentionDuration getRetention() {
        return retention;
    }
}

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

import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.internals.CloseableLogAppender;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;

import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

/**
 * Chronicle Queue implementation of LogAppender.
 *
 * @since 9.3
 */
public class ChronicleLogAppender<M extends Externalizable> implements CloseableLogAppender<M> {
    private static final Log log = LogFactory.getLog(ChronicleLogAppender.class);

    protected static final String PARTITION_PREFIX = "P-";

    protected static final String METADATA_FILE = "metadata.properties";

    protected static final int POLL_INTERVAL_MS = 100;

    protected static final int MAX_PARTITIONS = 100;

    public static final String MSG_KEY = "msg";

    // The block size determines the initial cq4 spare file size and the maximum message size.
    // A 4M block size creates a 5M file and enable a 1MB message
    public static final int CQ_BLOCK_SIZE = 4_194_304;

    public static final String RETENTION_KEY = "retention";

    public static final String PARTITIONS_KEY = "partitions";

    public static final String BLOCK_SIZE_KEY = "blockSize";

    protected final List<ChronicleQueue> partitions;

    protected final int nbPartitions;

    protected final File basePath;

    protected final int blockSize;

    protected final Name name;

    // keep track of created tailers to make sure they are closed before the log
    protected final ConcurrentLinkedQueue<ChronicleLogTailer<M>> tailers = new ConcurrentLinkedQueue<>();

    protected final ChronicleRetentionDuration retention;

    protected final Codec<M> codec;

    protected volatile boolean closed;

    /**
     * Open an existing Log
     */
    protected ChronicleLogAppender(Codec<M> codec, File basePath, ChronicleRetentionDuration retention) {
        if (!exists(basePath)) {
            throw new IllegalArgumentException("Cannot open Chronicle Queues, invalid path: " + basePath);
        }
        if (log.isDebugEnabled()) {
            log.debug("Opening: " + toString());
        }
        Objects.requireNonNull(codec);
        this.codec = codec;
        this.basePath = basePath;
        this.name = Name.ofId(basePath.getName());

        Path metadataPath = getMetadataPath();
        Properties metadata;
        if (metadataPath.toFile().exists()) {
            metadata = readMetadata(getMetadataPath());
        } else {
            // backward compatibility
            metadata = guessMetadata(retention);
        }
        ChronicleRetentionDuration storedRetention = new ChronicleRetentionDuration(
                metadata.getProperty(RETENTION_KEY));
        if (retention.disable()) {
            this.retention = ChronicleRetentionDuration.disableOf(storedRetention);
        } else if (retention.getRollCycle() == storedRetention.getRollCycle()) {
            this.retention = retention;
        } else {
            // we can change the number of retention cycles but not the roll cycle
            throw new IllegalArgumentException(String.format("Cannot open Log %s: expecting retention: %s got: %s",
                    name, storedRetention, retention));
        }
        this.nbPartitions = Integer.parseInt(metadata.getProperty(PARTITIONS_KEY));
        this.blockSize = Integer.parseInt(metadata.getProperty(BLOCK_SIZE_KEY));
        this.partitions = new ArrayList<>(nbPartitions);
        initPartitions(false);
    }

    /**
     * Create a new Log
     */
    protected ChronicleLogAppender(Codec<M> codec, File basePath, int size, ChronicleRetentionDuration retention) {
        if (size <= 0) {
            throw new IllegalArgumentException("Number of partitions must be > 0");
        }
        if (size > MAX_PARTITIONS) {
            throw new IllegalArgumentException(
                    String.format("Cannot create more than: %d partitions for log: %s, requested: %d", MAX_PARTITIONS,
                            basePath, size));
        }
        if (exists(basePath)) {
            throw new IllegalArgumentException("Cannot create Chronicle Queues, already exists: " + basePath);
        }
        if (!basePath.exists() && !basePath.mkdirs()) {
            throw new IllegalArgumentException("Invalid path to create Chronicle Queues: " + basePath);
        }
        Objects.requireNonNull(codec);
        this.nbPartitions = size;
        this.codec = codec;
        this.name = Name.ofId(basePath.getName());
        this.basePath = basePath;
        this.retention = retention;
        this.partitions = new ArrayList<>(nbPartitions);
        this.blockSize = CQ_BLOCK_SIZE;
        if (log.isDebugEnabled()) {
            log.debug("Creating: " + toString());
        }
        initPartitions(true);
        saveMetadata();
    }

    protected void initPartitions(boolean create) {
        for (int i = 0; i < nbPartitions; i++) {
            Path partitionPath = Paths.get(getBasePath(), String.format("%s%02d", PARTITION_PREFIX, i));
            if (create) {
                try {
                    Files.createDirectories(partitionPath);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Cannot create directory: " + partitionPath.toAbsolutePath(), e);
                }
            }
            ChronicleRetentionListener listener = null;
            SingleChronicleQueueBuilder builder = SingleChronicleQueueBuilder.binary(partitionPath)
                                                                             .rollCycle(retention.getRollCycle())
                                                                             .blockSize(blockSize);
            if (!retention.disable()) {
                listener = new ChronicleRetentionListener(retention);
                builder.storeFileListener(listener);
            }
            SingleChronicleQueue queue = builder.build();
            // we don't try to acquire an appender and pretouch because it causes troubles with countExcerpts
            // the cq4 cycle file will be created on first append
            partitions.add(queue);
            if (listener != null) {
                listener.setQueue(queue);
            }
        }
    }

    protected void saveMetadata() {
        Path metadata = getMetadataPath();
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("# Log created %s%n", Instant.now().toString()));
        builder.append(String.format("%s=%d%n", PARTITIONS_KEY, nbPartitions));
        builder.append(String.format("%s=%s%n", RETENTION_KEY, retention));
        builder.append(String.format("%s=%d%n", BLOCK_SIZE_KEY, blockSize));
        try {
            Files.write(metadata, builder.toString().getBytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to create metadata file: " + metadata, e);
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Created Log: %s%n%s", name, builder.toString()), new Throwable("here"));
        }
    }

    protected Path getMetadataPath() {
        return basePath.toPath().resolve(METADATA_FILE);
    }

    protected static Properties readMetadata(Path file) {
        Properties props = new Properties();
        try (InputStream stream = Files.newInputStream(file)) {
            props.load(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot open Log metadata file: " + file, e);
        }
        return props;
    }

    protected Properties guessMetadata(ChronicleRetentionDuration retention) {
        Properties props = new Properties();
        props.setProperty(PARTITIONS_KEY, Integer.toString(discoverPartitions(basePath.toPath())));
        props.setProperty(RETENTION_KEY, retention.getRetention());
        props.setProperty(BLOCK_SIZE_KEY, Integer.toString(CQ_BLOCK_SIZE));
        return props;
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
        return new ChronicleLogAppender<>(codec, basePath, size, ChronicleRetentionDuration.NONE);
    }

    /**
     * Open an existing log.
     */
    public static <M extends Externalizable> ChronicleLogAppender<M> open(Codec<M> codec, File basePath) {
        return new ChronicleLogAppender<>(codec, basePath, ChronicleRetentionDuration.NONE);
    }

    /**
     * Open an existing log.
     */
    public static <M extends Externalizable> ChronicleLogAppender<M> open(Codec<M> codec, File basePath,
            ChronicleRetentionDuration retention) {
        return new ChronicleLogAppender<>(codec, basePath, retention);
    }

    public String getBasePath() {
        return basePath.getPath();
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public int size() {
        return nbPartitions;
    }

    @Override
    public LogOffset append(int partition, M message) {
        ExcerptAppender appender = partitions.get(partition).acquireAppender();
        try {
            if (NO_CODEC.equals(codec)) {
                // default format for backward compatibility
                appender.writeDocument(w -> w.write(MSG_KEY).object(message));
            } else {
                appender.writeDocument(w -> w.write().bytes(codec.encode(message)));
            }
        } catch (DecoratedBufferOverflowException e) {
            throw new StreamRuntimeException(e);
        }
        long offset = appender.lastIndexAppended();
        LogOffset ret = new LogOffsetImpl(name, partition, offset);
        if (log.isDebugEnabled()) {
            log.debug(String.format("append to %s, value: %s", ret, message));
        }
        return ret;
    }

    public LogTailer<M> createTailer(LogPartition partition, Name group, Codec<M> codec) {
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
    public boolean waitFor(LogOffset offset, Name group, Duration timeout) throws InterruptedException {
        boolean ret;
        long offsetPosition = offset.offset();
        int partition = offset.partition().partition();
        try (ChronicleLogOffsetTracker offsetTracker = new ChronicleLogOffsetTracker(basePath.toString(), partition,
                group, ChronicleRetentionDuration.disableOf(retention))) {
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

    public static int partitions(Path basePath) {
        Path metadataPath = basePath.resolve(METADATA_FILE);
        if (metadataPath.toFile().exists()) {
            return Integer.parseInt(readMetadata(metadataPath).getProperty(PARTITIONS_KEY));
        }
        // backward compatibility before metadata file
        return discoverPartitions(basePath);
    }

    public static int discoverPartitions(Path basePath) {
        try (Stream<Path> paths = Files.list(basePath)) {
            int ret = (int) paths.filter(ChronicleLogAppender::isPartitionDirectory).count();
            if (ret == 0) {
                throw new IOException("No chronicles queues file found");
            }
            return ret;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid basePath for queue: " + basePath, e);
        }
    }

    protected static boolean isPartitionDirectory(Path path) {
        return path.toFile().isDirectory() && path.getFileName().toString().startsWith(PARTITION_PREFIX);
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

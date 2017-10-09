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

import static net.openhft.chronicle.queue.impl.single.SingleChronicleQueue.SUFFIX;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.RollCycle;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.RollingResourcesCache;
import net.openhft.chronicle.queue.impl.StoreFileListener;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

/**
 * Chronicle Queue implementation of LogAppender.
 *
 * @since 9.3
 */
public class ChronicleLogAppender<M extends Externalizable> implements LogAppender<M>, StoreFileListener {
    protected static final String QUEUE_PREFIX = "Q-";

    protected static final int POLL_INTERVAL_MS = 100;

    protected static final String SECOND_ROLLING_PERIOD = "s";

    protected static final String MINUTE_ROLLING_PERIOD = "m";

    protected static final String HOUR_ROLLING_PERIOD = "h";

    protected static final String DAY_ROLLING_PERIOD = "d";

    private static final Log log = LogFactory.getLog(ChronicleLogAppender.class);

    protected final List<ChronicleQueue> queues;

    protected final int nbQueues;

    protected final File basePath;

    protected final String name;

    // keep track of created tailers to make sure they are closed before the log
    protected final ConcurrentLinkedQueue<ChronicleLogTailer<M>> tailers = new ConcurrentLinkedQueue<>();

    protected int retentionNbCycles;

    protected boolean closed = false;

    protected ChronicleLogAppender(File basePath, int size, String retentionDuration) {
        if (size == 0) {
            // open
            if (!exists(basePath)) {
                // TODO: do we need to log.error (same below)
                String msg = "Cannot open Chronicle Queues, invalid path: " + basePath;
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
            this.nbQueues = findNbQueues(basePath);
        } else {
            // creation
            if (exists(basePath)) {
                String msg = "Cannot create Chronicle Queues, already exists: " + basePath;
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
            if (!basePath.exists() && !basePath.mkdirs()) {
                String msg = "Cannot create Chronicle Queues in: " + basePath;
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
            this.nbQueues = size;
        }
        this.name = basePath.getName();
        this.basePath = basePath;

        if (retentionDuration != null) {
            retentionNbCycles = Integer.parseInt(retentionDuration.substring(0, retentionDuration.length() - 1));
        }

        RollCycle rollCycle = getRollCycle(retentionDuration);

        queues = new ArrayList<>(this.nbQueues);
        if (log.isDebugEnabled()) {
            log.debug(String.format("%s chronicle queue: %s, path: %s, size: %d", (size == 0) ? "Opening" : "Creating",
                    name, basePath, nbQueues));
        }
        for (int i = 0; i < nbQueues; i++) {
            File path = new File(basePath, String.format("%s%02d", QUEUE_PREFIX, i));
            ChronicleQueue queue = SingleChronicleQueueBuilder.binary(path)
                                                              .rollCycle(rollCycle)
                                                              .storeFileListener(this)
                                                              .build();
            queues.add(queue);
            // touch the queue so we can count them even if they stay empty.
            try {
                Files.createDirectories(queue.file().toPath());
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot create directory: " + queue.file().getAbsolutePath(), e);
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
    public static <M extends Externalizable> ChronicleLogAppender<M> create(File basePath, int size,
            String retentionPolicy) {
        return new ChronicleLogAppender<>(basePath, size, retentionPolicy);
    }

    /**
     * Create a new log.
     */
    public static <M extends Externalizable> ChronicleLogAppender<M> create(File basePath, int size) {
        return new ChronicleLogAppender<>(basePath, size, ChronicleLogManager.DEFAULT_RETENTION_DURATION);
    }

    /**
     * Open an existing log.
     */
    public static <M extends Externalizable> ChronicleLogAppender<M> open(File basePath) {
        return new ChronicleLogAppender<>(basePath, 0, ChronicleLogManager.DEFAULT_RETENTION_DURATION);
    }

    /**
     * Open an existing log.
     */
    public static <M extends Externalizable> ChronicleLogAppender<M> open(File basePath, String retentionDuration) {
        return new ChronicleLogAppender<>(basePath, 0, retentionDuration);
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
        return nbQueues;
    }

    @Override
    public LogOffset append(int partition, M message) {
        ExcerptAppender appender = queues.get(partition).acquireAppender();
        appender.writeDocument(w -> w.write("msg").object(message));
        long offset = appender.lastIndexAppended();
        LogOffset ret = new LogOffsetImpl(name, partition, offset);
        if (log.isDebugEnabled()) {
            log.debug(String.format("append to %s, value: %s", ret, message));
        }
        return ret;
    }

    public LogTailer<M> createTailer(LogPartition partition, String group) {
        return addTailer(new ChronicleLogTailer<>(basePath.toString(), queues.get(partition.partition()).createTailer(),
                partition, group));
    }

    public long endOffset(int partition) {
        return queues.get(partition).createTailer().toEnd().index();
    }

    public long firstOffset(int partition) {
        long ret = queues.get(partition).firstIndex();
        if (ret == Long.MAX_VALUE) {
            return 0;
        }
        return ret;
    }

    public long countMessages(int partition, long lowerOffset, long upperOffset) {
        long ret;
        SingleChronicleQueue queue = (SingleChronicleQueue) queues.get(partition);
        try {
            ret = queue.countExcerpts(lowerOffset, upperOffset);
        } catch (IllegalStateException e) {
            // 'file not found' for the lowerCycle
            return 0;
        }
        // System.out.println("partition: " + partition + ", count from " + lowerOffset + " to " + upperOffset + " = " +
        // ret);
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

    protected boolean isProcessed(ChronicleLogOffsetTracker tracker, long offset) {
        long last = tracker.readLastCommittedOffset();
        return last > 0 && last >= offset;
    }

    @Override
    public void close() {
        log.debug("Closing queue");
        tailers.stream().filter(Objects::nonNull).forEach(ChronicleLogTailer::close);
        tailers.clear();
        queues.stream().filter(Objects::nonNull).forEach(ChronicleQueue::close);
        queues.clear();
        closed = true;
    }

    protected int findNbQueues(File basePath) {
        int ret;
        try (Stream<Path> paths = Files.list(basePath.toPath())) {
            ret = (int) paths.filter(
                    path -> (Files.isDirectory(path) && path.getFileName().toString().startsWith(QUEUE_PREFIX)))
                             .count();
            if (ret == 0) {
                throw new IOException("No chronicles queues file found");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid basePath for queue: " + basePath, e);
        }
        return ret;
    }

    protected RollCycle getRollCycle(String retentionDuration) {
        String rollingPeriod = retentionDuration.substring(retentionDuration.length() - 1);
        switch (rollingPeriod) {
        case SECOND_ROLLING_PERIOD:
            return RollCycles.TEST_SECONDLY;
        case MINUTE_ROLLING_PERIOD:
            return RollCycles.MINUTELY;
        case HOUR_ROLLING_PERIOD:
            return RollCycles.HOURLY;
        case DAY_ROLLING_PERIOD:
            return RollCycles.DAILY;
        default:
            String msg = "Unknown rolling period: " + rollingPeriod + " for Log: " + name();
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    protected int findQueueIndex(File queueFile) {
        String queueDirName = queueFile.getParentFile().getName();
        return Integer.parseInt(queueDirName.substring(queueDirName.length() - 2));
    }

    @Override
    public void onAcquired(int cycle, File file) {
        if (log.isDebugEnabled()) {
            log.debug("New file created: " + file + " on cycle: " + cycle);
        }

        SingleChronicleQueue queue = (SingleChronicleQueue) queues.get(findQueueIndex(file));

        int lowerCycle = queue.firstCycle();
        int upperCycle = cycle - retentionNbCycles;

        purgeQueue(lowerCycle, upperCycle, queue);

    }

    /**
     * Files in queue older than the current date minus the retention duration are candidates for purging, knowing that
     * the more recent files should be kept to ensure no data loss (for example after an interruption longer than the
     * retention duration).
     */
    protected void purgeQueue(int lowerCycle, int upperCycle, SingleChronicleQueue queue) {
        // TODO: refactor this using new chronicle-queue lib methods
        File[] files = queue.file().listFiles();

        if (files != null && lowerCycle < upperCycle) {
            RollingResourcesCache cache = new RollingResourcesCache(queue.rollCycle(), queue.epoch(),
                    name -> new File(queue.file().getAbsolutePath(), name + SUFFIX),
                    f -> FilenameUtils.removeExtension(f.getName()));

            Arrays.stream(files)
                  .sorted(Comparator.comparingLong(cache::toLong)) // Order files by cycles
                  .limit(files.length - retentionNbCycles) // Keep the 'retentionNbCycles' more recent files
                  .filter(f -> cache.parseCount(FilenameUtils.removeExtension(f.getName())) < upperCycle)
                  .forEach(f -> {
                      if (f.delete()) {
                          log.info("Queue file deleted: " + f.getAbsolutePath());
                      }
                  });
        }
    }

    @Override
    public void onReleased(int cycle, File file) {

    }

    @Override
    public String toString() {
        return "ChronicleLogAppender{" + "nbQueues=" + nbQueues + ", basePath=" + basePath + ", name='" + name + '\''
                + ", retentionNbCycles=" + retentionNbCycles + ", closed=" + closed + '}';
    }
}

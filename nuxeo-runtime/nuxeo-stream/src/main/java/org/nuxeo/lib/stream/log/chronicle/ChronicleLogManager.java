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

import static org.apache.commons.io.FileUtils.deleteDirectory;

import java.io.Externalizable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceListener;
import org.nuxeo.lib.stream.log.internals.AbstractLogManager;

/**
 * @since 9.3
 */
public class ChronicleLogManager extends AbstractLogManager {
    /**
     * Default retention duration for log
     */
    public static final String DEFAULT_RETENTION_DURATION = "4d";

    private static final Log log = LogFactory.getLog(ChronicleLogManager.class);

    protected final Path basePath;

    protected final String retentionDuration;

    public ChronicleLogManager(Path basePath) {
        this.basePath = basePath;
        this.retentionDuration = DEFAULT_RETENTION_DURATION;
    }

    /**
     * Constructor
     *
     * @param basePath the base path.
     * @param retentionDuration the retention duration. It is the time period the queue files will be retained. Once the
     *            retention duration expires, the older files are candidates for being purged. The property can be
     *            expressed as: 15s, 30m, 1h, 4d ... (where 's' is expressing a duration in seconds, 'm' in minutes,'h'
     *            in hours and 'd' in days)
     */
    public ChronicleLogManager(Path basePath, String retentionDuration) {
        this.basePath = basePath;
        this.retentionDuration = retentionDuration == null ? DEFAULT_RETENTION_DURATION : retentionDuration;
    }

    protected static void deleteQueueBasePath(Path basePath) {
        try {
            log.info("Removing Chronicle Queues directory: " + basePath);
            // Performs a recursive delete if the directory contains only chronicles files
            try (Stream<Path> paths = Files.list(basePath)) {
                int count = (int) paths.filter(path -> (Files.isRegularFile(path) && !path.toString().endsWith(".cq4")))
                                       .count();
                if (count > 0) {
                    String msg = "ChronicleLog basePath: " + basePath
                            + " contains unknown files, please choose another basePath";
                    log.error(msg);
                    throw new IllegalArgumentException(msg);
                }
            }
            deleteDirectory(basePath.toFile());
        } catch (IOException e) {
            String msg = "Cannot remove Chronicle Queues directory: " + basePath + " " + e.getMessage();
            log.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

    public String getBasePath() {
        return basePath.toAbsolutePath().toString();
    }

    @Override
    public boolean exists(String name) {
        try {
            return Files.list(basePath.resolve(name)).count() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void create(String name, int size) {
        ChronicleLogAppender.create(basePath.resolve(name).toFile(), size, retentionDuration).close();
    }

    @Override
    public boolean delete(String name) {
        Path path = basePath.resolve(name);
        if (Files.isDirectory(path)) {
            deleteQueueBasePath(path);
            return true;
        }
        return false;
    }

    protected LogLag getLagForPartition(String name, int partition, String group) {
        long pos;
        Path path = basePath.resolve(name);
        try (ChronicleLogOffsetTracker offsetTracker = new ChronicleLogOffsetTracker(path.toString(), partition,
                group)) {
            pos = offsetTracker.readLastCommittedOffset();
        }
        ChronicleLogAppender appender = (ChronicleLogAppender) getAppender(name);
        if (pos == 0) {
            pos = appender.firstOffset(partition);
        }
        long end = appender.endOffset(partition);
        long lag = appender.countMessages(partition, pos, end);
        long firstOffset = appender.firstOffset(partition);
        long endMessages = appender.countMessages(partition, firstOffset, end);
        return new LogLag(pos, end, lag, endMessages);
    }

    @Override
    public List<LogLag> getLagPerPartition(String name, String group) {
        int size = getAppender(name).size();
        List<LogLag> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ret.add(getLagForPartition(name, i, group));
        }
        return ret;
    }

    @Override
    public String toString() {
        return "ChronicleLogManager{" + "basePath=" + basePath + ", retentionDuration='" + retentionDuration + '\''
                + '}';
    }

    @Override
    public List<String> listAll() {
        try {
            return Files.list(basePath).filter(Files::isDirectory).map(Path::getFileName).map(Path::toString).collect(
                    Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base path: " + basePath, e);
        }
    }

    @Override
    public List<String> listConsumerGroups(String name) {
        Path logRoot = basePath.resolve(name);
        if (!Files.exists(logRoot)) {
            throw new IllegalArgumentException("Unknown Log: " + name);
        }
        try {
            return Files.list(logRoot)
                        .filter(Files::isDirectory)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(ChronicleLogOffsetTracker::isOffsetTracker)
                        .map(ChronicleLogOffsetTracker::getGroupFromDirectory)
                        .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot access Log: " + name, e);
        }
    }

    @Override
    public <M extends Externalizable> LogAppender<M> createAppender(String name) {
        return ChronicleLogAppender.open(basePath.resolve(name).toFile(), retentionDuration);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <M extends Externalizable> LogTailer<M> acquireTailer(Collection<LogPartition> partitions, String group) {
        Collection<ChronicleLogTailer<M>> pTailers = new ArrayList<>(partitions.size());
        partitions.forEach(partition -> pTailers.add(
                (ChronicleLogTailer<M>) ((ChronicleLogAppender<M>) getAppender(partition.name())).createTailer(
                        partition, group)));
        if (pTailers.size() == 1) {
            return pTailers.iterator().next();
        }
        return new ChronicleCompoundLogTailer<>(pTailers, group);
    }

    @Override
    protected <M extends Externalizable> LogTailer<M> doSubscribe(String group, Collection<String> names,
            RebalanceListener listener) {
        throw new UnsupportedOperationException("subscribe is not supported by Chronicle implementation");

    }

}

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
import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;
import static org.nuxeo.lib.stream.log.chronicle.ChronicleLogAppender.METADATA_FILE;

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
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceListener;
import org.nuxeo.lib.stream.log.internals.AbstractLogManager;
import org.nuxeo.lib.stream.log.internals.CloseableLogAppender;

/**
 * @since 9.3
 */
public class ChronicleLogManager extends AbstractLogManager {
    private static final Log log = LogFactory.getLog(ChronicleLogManager.class);

    protected final Path basePath;

    protected final ChronicleRetentionDuration retention;

    public ChronicleLogManager(Path basePath) {
        this(basePath, null);
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
        this.retention = new ChronicleRetentionDuration(retentionDuration);
    }

    protected static void deleteQueueBasePath(Path basePath) {
        try {
            log.info("Removing Chronicle Queues directory: " + basePath);
            // Performs a recursive delete if the directory contains only chronicles files
            try (Stream<Path> paths = Files.list(basePath)) {
                int count = (int) paths.filter(path -> (path.toFile().isFile() && !isChronicleLogFile(path))).count();
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

    protected static boolean isChronicleLogFile(Path path) {
        String filename = path.getFileName().toString();
        return filename.endsWith(".cq4") || filename.endsWith(".cq4t") || METADATA_FILE.equals(filename);
    }

    public String getBasePath() {
        return basePath.toAbsolutePath().toString();
    }

    @Override
    public boolean exists(String name) {
        try (Stream<Path> paths = Files.list(basePath.resolve(name))) {
            return paths.count() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void create(String name, int size) {
        ChronicleLogAppender.create(NO_CODEC, basePath.resolve(name).toFile(), size, retention).close();
    }

    @Override
    protected int getSize(String name) {
        return ChronicleLogAppender.partitions(basePath.resolve(name));
    }

    @Override
    public boolean delete(String name) {
        Path path = basePath.resolve(name);
        if (path.toFile().isDirectory()) {
            deleteQueueBasePath(path);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    protected LogLag getLagForPartition(String name, int partition, String group) {
        long pos;
        Path path = basePath.resolve(name);
        if (!ChronicleLogOffsetTracker.exists(path, group)) {
            pos = 0;
        } else {
            try (ChronicleLogOffsetTracker offsetTracker = new ChronicleLogOffsetTracker(path.toString(), partition,
                    group, ChronicleRetentionDuration.disableOf(retention))) {
                pos = offsetTracker.readLastCommittedOffset();
            }
        }
        try (ChronicleLogAppender<Externalizable> appender = ChronicleLogAppender.open(NO_CODEC,
                basePath.resolve(name).toFile())) {
            // this trigger an acquire/release on cycle
            long end = appender.endOffset(partition);
            if (pos == 0) {
                pos = appender.firstOffset(partition);
            }
            long lag = appender.countMessages(partition, pos, end);
            long firstOffset = appender.firstOffset(partition);
            long endMessages = appender.countMessages(partition, firstOffset, end);
            return new LogLag(pos, end, lag, endMessages);
        }
    }

    @Override
    public List<LogLag> getLagPerPartition(String name, String group) {
        int size = size(name);
        List<LogLag> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ret.add(getLagForPartition(name, i, group));
        }
        return ret;
    }

    @Override
    public String toString() {
        return "ChronicleLogManager{" + "basePath=" + basePath + ", retention='" + retention + '\'' + '}';
    }

    @Override
    public List<String> listAll() {
        try (Stream<Path> paths = Files.list(basePath)) {
            return paths.filter(Files::isDirectory)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base path: " + basePath, e);
        }
    }

    @Override
    public List<String> listConsumerGroups(String name) {
        Path logRoot = basePath.resolve(name);
        if (!logRoot.toFile().exists()) {
            throw new IllegalArgumentException("Unknown Log: " + name);
        }
        try (Stream<Path> paths = Files.list(logRoot)) {
            return paths.filter(Files::isDirectory)
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
    public <M extends Externalizable> CloseableLogAppender<M> createAppender(String name, Codec<M> codec) {
        return ChronicleLogAppender.open(codec, basePath.resolve(name).toFile(), retention);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <M extends Externalizable> LogTailer<M> doCreateTailer(Collection<LogPartition> partitions, String group,
            Codec<M> codec) {
        Collection<ChronicleLogTailer<M>> pTailers = new ArrayList<>(partitions.size());
        partitions.forEach(partition -> pTailers.add(
                (ChronicleLogTailer<M>) ((ChronicleLogAppender<M>) getAppender(partition.name(), codec)).createTailer(
                        partition, group, codec)));
        if (pTailers.size() == 1) {
            return pTailers.iterator().next();
        }
        return new ChronicleCompoundLogTailer<>(pTailers, group);
    }

    @Override
    protected <M extends Externalizable> LogTailer<M> doSubscribe(String group, Collection<String> names,
            RebalanceListener listener, Codec<M> codec) {
        throw new UnsupportedOperationException("subscribe is not supported by Chronicle implementation");

    }

}

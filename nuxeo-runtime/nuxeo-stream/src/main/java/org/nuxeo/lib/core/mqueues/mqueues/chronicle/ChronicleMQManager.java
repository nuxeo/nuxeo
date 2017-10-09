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
package org.nuxeo.lib.core.mqueues.mqueues.chronicle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.core.mqueues.mqueues.MQAppender;
import org.nuxeo.lib.core.mqueues.mqueues.MQLag;
import org.nuxeo.lib.core.mqueues.mqueues.MQPartition;
import org.nuxeo.lib.core.mqueues.mqueues.MQRebalanceListener;
import org.nuxeo.lib.core.mqueues.mqueues.MQTailer;
import org.nuxeo.lib.core.mqueues.mqueues.internals.AbstractMQManager;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * @since 9.2
 */
public class ChronicleMQManager extends AbstractMQManager {
    private static final Log log = LogFactory.getLog(ChronicleMQManager.class);

    /**
     * Default retention duration for queue files
     */
    public static final String DEFAULT_RETENTION_DURATION = "4d";

    protected final Path basePath;

    protected final String retentionDuration;

    public ChronicleMQManager(Path basePath) {
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
    public ChronicleMQManager(Path basePath, String retentionDuration) {
        this.basePath = basePath;
        this.retentionDuration = retentionDuration == null ? DEFAULT_RETENTION_DURATION : retentionDuration;
    }

    public String getBasePath() {
        return basePath.toAbsolutePath().toString();
    }

    @Override
    public boolean exists(String name) {
        File path = new File(basePath.toFile(), name);
        //noinspection ConstantConditions
        return path.isDirectory() && path.list().length > 0;
    }

    @Override
    public void create(String name, int size) {
        ChronicleMQAppender cq = ChronicleMQAppender.create(new File(basePath.toFile(), name), size, retentionDuration);
        try {
            cq.close();
        } catch (Exception e) {
            throw new RuntimeException("Can not create and close " + name, e);
        }
    }

    @Override
    public boolean delete(String name) {
        File path = new File(basePath.toFile(), name);
        if (path.isDirectory()) {
            deleteQueueBasePath(path);
            return true;
        }
        return false;
    }

    protected MQLag getLagForPartition(String name, int partition, String group) {
        long pos = 0;
        File path = new File(basePath.toFile(), name);
        try (ChronicleMQOffsetTracker offsetTracker = new ChronicleMQOffsetTracker(path.toString(), partition, group)) {
            pos = offsetTracker.readLastCommittedOffset();
        }
        ChronicleMQAppender appender = (ChronicleMQAppender) getAppender(name);
        if (pos == 0) {
            pos = appender.firstOffset(partition);
        }
        long end = appender.endOffset(partition);
        long lag = appender.countMessages(partition, pos, end);
        long firstOffset = appender.firstOffset(partition);
        long endMessages = appender.countMessages(partition, firstOffset, end);
        return new MQLag(pos, end, lag, endMessages);
    }

    @Override
    public List<MQLag> getLagPerPartition(String name, String group) {
        int size = getAppender(name).size();
        List<MQLag> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ret.add(getLagForPartition(name, i, group));
        }
        return ret;
    }

    @Override
    public String toString() {
        return "ChronicleMQManager{" +
                "basePath=" + basePath +
                ", retentionDuration='" + retentionDuration + '\'' +
                '}';
    }

    @Override
    public List<String> listAll() {
        if (!basePath.toFile().exists() || !basePath.toFile().isDirectory()) {
            throw new IllegalArgumentException("Invalid base path: " + basePath);
        }
        return Arrays.asList(basePath.toFile().list((dir, name) -> new File(dir, name).isDirectory()));
    }

    @Override
    public List<String> listConsumerGroups(String name) {
        File mqRoot = new File(basePath.toFile(), name);
        if (!exists(name)) {
            throw new IllegalArgumentException("Unknown MQueue: " + name);
        }
        return Arrays.stream(mqRoot.list((dir, rep) -> new File(dir, rep).isDirectory() &&
                ChronicleMQOffsetTracker.isOffsetTracker(rep))).map(ChronicleMQOffsetTracker::getGroupFromDirectory).collect(Collectors.toList());
    }

    @Override
    public <M extends Externalizable> MQAppender<M> createAppender(String name) {
        return ChronicleMQAppender.open(new File(basePath.toFile(), name), retentionDuration);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <M extends Externalizable> MQTailer<M> acquireTailer(Collection<MQPartition> partitions, String group) {
        Collection<ChronicleMQTailer<M>> pTailers = new ArrayList<>(partitions.size());
        partitions.forEach(partition -> pTailers.add((ChronicleMQTailer<M>) ((ChronicleMQAppender<M>) getAppender(partition.name())).createTailer(partition, group)));
        if (pTailers.size() == 1) {
            return pTailers.iterator().next();
        }
        return new ChronicleCompoundMQTailer<>(pTailers, group);
    }

    @Override
    protected <M extends Externalizable> MQTailer<M> doSubscribe(String group, Collection<String> names, MQRebalanceListener listener) {
        throw new UnsupportedOperationException("subscribe is not supported by Chronicle implementation");

    }

    protected static void deleteQueueBasePath(File basePath) {
        try {
            log.info("Removing Chronicle Queues directory: " + basePath);
            // Performs a recursive delete if the directory contains only chronicles files
            try (Stream<Path> paths = Files.list(basePath.toPath())) {
                int count = (int) paths.filter(path -> (Files.isRegularFile(path) && !path.toString().endsWith(".cq4"))).count();
                if (count > 0) {
                    String msg = "ChronicleMQueue basePath: " + basePath + " contains unknown files, please choose another basePath";
                    log.error(msg);
                    throw new IllegalArgumentException(msg);
                }
            }
            deleteDirectory(basePath);
        } catch (IOException e) {
            String msg = "Can not remove Chronicle Queues directory: " + basePath + " " + e.getMessage();
            log.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

}

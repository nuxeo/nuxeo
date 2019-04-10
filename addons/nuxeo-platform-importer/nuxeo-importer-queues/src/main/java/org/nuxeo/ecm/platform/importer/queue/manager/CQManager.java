package org.nuxeo.ecm.platform.importer.queue.manager;/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bdelbosc
 */


import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.jetbrains.annotations.Nullable;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * @since 8.10
 */
public class CQManager extends AbstractQueuesManager {

    final List<ChronicleQueue> queues;
    final List<ExcerptAppender> appenders;
    final List<ExcerptTailer> tailers;

    public CQManager(ImporterLogger logger, int queuesNb) {
        this(logger, queuesNb, false);
    }

    public CQManager(ImporterLogger logger, int queuesNb, boolean append) {
        this(new File(System.getProperty("java.io.tmpdir"), "CQ"), logger, queuesNb, append);
    }

    public CQManager(File basePath, ImporterLogger logger, int queuesNb, boolean append) {
        super(logger, queuesNb);
        queues = new ArrayList<>(queuesNb);
        appenders = new ArrayList<>(queuesNb);
        tailers = new ArrayList<>(queuesNb);
        if (!append) {
            try {
                logger.info("Clearing previous queues in: " + basePath);
                deleteDirectory(basePath);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        logger.info("Using chronicle queues in: " + basePath);
        basePath.mkdirs();

        for (int i = 0; i < queuesNb; i++) {
            File path = new File(basePath, "Q" + i);
            ChronicleQueue queue = SingleChronicleQueueBuilder.binary(path).build();
            appenders.add(queue.acquireAppender());
            tailers.add(queue.createTailer().toEnd());
        }
    }

    @Override
    public void put(int queue, SourceNode node) throws InterruptedException {
        appenders.get(queue).writeDocument(w -> w.write("node").object(node));
    }

    @Override
    public SourceNode poll(int queue) {
        try {
            return poll(queue, 5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error("poll timeout", e);
            ExceptionUtils.checkInterrupt(e);
        }
        return null;
    }

    @Nullable
    private SourceNode get(int queue) {
        final SourceNode[] ret = new SourceNode[1];
        if (tailers.get(queue).readDocument(w -> {
            ret[0] = (SourceNode) w.read("node").object();
        })) {
            return ret[0];
        }
        return null;
    }

    @Override
    public SourceNode poll(int queue, long timeout, TimeUnit unit) throws InterruptedException {
        SourceNode ret = get(queue);
        if (ret != null) {
            return ret;
        }
        final long deadline = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, unit);
        while (ret == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            ret = get(queue);
        }
        return ret;
    }

    @Override
    public boolean isEmpty(int queue) {
        return !tailers.get(queue).readingDocument().isPresent();
    }

    @Override
    public int size(int queue) {
        return 0;
    }

    @Override
    public void close() {
        queues.stream().filter(Objects::nonNull).forEach(ChronicleQueue::close);
        appenders.clear();
        tailers.clear();
        queues.clear();
    }
}

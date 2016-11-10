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


import com.sun.corba.se.impl.protocol.giopmsgheaders.MessageHandler;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.awt.SystemColor.text;

/**
 * @since 8.10
 */
public class CQManager extends AbstractQueuesManager {

    final List<ChronicleQueue> queues;
    final List<ExcerptAppender> appenders;
    final List<ExcerptTailer> tailers;

    public CQManager(ImporterLogger logger, int queuesNb) {
        super(logger, queuesNb);
        queues = new ArrayList<>(queuesNb);
        appenders = new ArrayList<>(queuesNb);
        tailers = new ArrayList<>(queuesNb);

        // Create a path for the queue
        File basePath = new File(System.getProperty("java.io.tmpdir"), "IQ");
        basePath.mkdirs();
        System.out.println("QUEUE " + basePath);
        for (int i = 0; i < queuesNb; i++) {
            try (ChronicleQueue queue = SingleChronicleQueueBuilder.binary(new File(basePath, "Q" + i)).build()) {
                appenders.add(queue.acquireAppender());
                tailers.add(queue.createTailer());
            }
        }
    }


    @Override
    public void put(int queue, SourceNode node) throws InterruptedException {
        try (final DocumentContext dc = appenders.get(queue).writingDocument()) {
            dc.wire().write().object(node);
        }
    }

    @Override
    public SourceNode poll(int queue) {
        try (DocumentContext dc = tailers.get(queue).readingDocument()) {
            if (dc.isPresent()) {
                try {
                    return (SourceNode) dc.wire().objectInput().readObject();
                } catch (ClassNotFoundException e) {
                    log.error("Can not read object" + e.getMessage(), e);
                } catch (IOException e) {
                    log.error("Can not read object" + e.getMessage(), e);
                }
            }
        }
        return null;
    }

    @Override
    public SourceNode poll(int queue, long timeout, TimeUnit unit) throws InterruptedException {
        return poll(queue);
    }

    @Override
    public boolean isEmpty(int queue) {
        return ! tailers.get(queue).readingDocument().isPresent();
    }

    @Override
    public int size(int queue) {
        return 0;
    }

    @Override
    public int dispatch(SourceNode node) throws InterruptedException {
        return 0;
    }

    @Override
    public int getNBConsumers() {
        return 0;
    }

    @Override
    public BlockingQueue<SourceNode> getQueue(int idx) {
        return null;
    }



}

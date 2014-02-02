/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work.redis;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.NuxeoBlockingQueue;
import org.nuxeo.ecm.core.work.WorkHolder;
import org.nuxeo.ecm.core.work.api.Work;

/**
 * Redis-based {@link BlockingQueue}.
 * <p>
 * It has unlimited capacity, so never blocks on {@link #put} and {@link #offer}
 * always returns {@code true}.
 *
 * @since 5.8
 */
public class RedisBlockingQueue extends NuxeoBlockingQueue {

    private static final Log log = LogFactory.getLog(RedisBlockingQueue.class);

    protected final String queueId;

    protected final RedisWorkQueuing queuing;

    public RedisBlockingQueue(String queueId, RedisWorkQueuing queuing) {
        this.queueId = queueId;
        this.queuing = queuing;
    }

    @Override
    public int getQueueSize() {
        return queuing.getScheduledSize(queueId);
    }

    @Override
    public void putElement(Runnable r) {
        Work work = WorkHolder.getWork(r);
        try {
            queuing.addScheduledWork(queueId, work);
        } catch (IOException e) {
            log.error("Failed to add Work: " + work, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Runnable pollElement() {
        try {
            Work work = queuing.removeScheduledWork(queueId);
            if (work != null) {
                log.debug("Remove scheduled " + work);
            }
            return work == null ? null : new WorkHolder(work);
        } catch (IOException e) {
            log.error("Failed to remove Work from queue: " + queueId, e);
            throw new RuntimeException(e);
        }
    }

}

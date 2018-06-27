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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.StreamRuntimeException;

import net.openhft.chronicle.queue.impl.StoreFileListener;
import net.openhft.chronicle.queue.impl.WireStore;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;

/**
 * @since 9.3
 */
public class ChronicleRetentionListener implements StoreFileListener {
    private static final Log log = LogFactory.getLog(ChronicleRetentionListener.class);

    protected final ChronicleRetentionDuration retention;

    protected SingleChronicleQueue queue;

    protected long purgedStamp;

    public ChronicleRetentionListener(ChronicleRetentionDuration retention) {
        this.retention = retention;
    }

    public void setQueue(SingleChronicleQueue queue) {
        this.queue = queue;
    }

    @Override
    public void onAcquired(int cycle, File file) {
        if (queue == null || retention.disable()) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Acquire Chronicle file: %s, cycle: %s", file, cycle));
        }
    }

    public synchronized void purge() {
        if (queue == null || queue.isClosed() || retention.disable()) {
            return;
        }
        List<Integer> cycles = getAllCycles();
        int cyclesToRemove = cycles.size() - retention.getRetentionCycles();
        if (cyclesToRemove <= 0) {
            return;
        }
        purgedStamp = System.currentTimeMillis();
        cycles.subList(0, cyclesToRemove).forEach(this::dropCycle);
        // this is needed to update first cycle, it calls directoryListing.refresh()
        queue.createTailer();
    }

    protected void dropCycle(Integer cycle) {
        File file = getFileForCycle(cycle);
        if (!file.exists()) {
            return;
        }
        log.info(String.format("Deleting Chronicle file: %s according to retention: %s", file.getAbsolutePath(),
                retention));
        try {
            Files.delete(file.toPath());
            log.debug(file + " deleted");
        } catch (IOException | SecurityException e) {
            log.warn(String.format("Unable to delete Chronicle file: %s, %s", file.getAbsolutePath(), e.getMessage()));
        }
    }

    protected List<Integer> getAllCycles() {
        List<Integer> ret = new ArrayList<>();
        try {
            NavigableSet<Long> allCycles = queue.listCyclesBetween(queue.firstCycle(), queue.lastCycle());
            allCycles.iterator().forEachRemaining(cycle -> ret.add(cycle.intValue()));
            return ret;
        } catch (ParseException e) {
            throw new StreamRuntimeException("Fail to list cycles for queue: " + queue, e);
        }
    }

    protected File getFileForCycle(int cycle) {
        WireStore store = queue.storeForCycle(cycle, queue.epoch(), false);
        return (store != null) ? store.file() : null;
    }

    @Override
    public void onReleased(int cycle, File file) {
        if (queue == null || queue.isClosed() || retention.disable()) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Release Chronicle file: %s, cycle: %d", file, cycle));
        }
        if (checkPurge()) {
            purge();
        }
    }

    protected boolean checkPurge() {
        // there is no need to purge more than the cycle length (which is duration in ms)
        if (System.currentTimeMillis() - purgedStamp >= retention.getRollCycle().length()) {
            return true;
        }
        if (log.isDebugEnabled()) {
            log.debug("Skipping purge already done in within cycle duration: " + purgedStamp);
        }
        return false;
    }

}

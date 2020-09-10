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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.nuxeo.lib.stream.StreamRuntimeException;

import net.openhft.chronicle.queue.impl.StoreFileListener;
import net.openhft.chronicle.queue.impl.WireStore;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueStore;

/**
 * @since 9.3
 */
public class ChronicleRetentionListener implements StoreFileListener {

    private static final Logger log = LogManager.getLogger(ChronicleRetentionListener.class);

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
        log.debug("Acquire Chronicle file: {}, cycle: {}", file, cycle);

    }

    public synchronized void purge() {
        if (queue == null || queue.isClosed() || retention.disable() || !queue.file().exists()) {
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
        queue.createTailer().close();
    }

    protected void dropCycle(Integer cycle) {
        SingleChronicleQueueStore store = queue.storeForCycle(cycle, queue.epoch(), false, null);
        if (store == null) {
            return;
        }
        File file = store.file();
        if (file == null || !file.exists()) {
            return;
        }
        log.info("Deleting Chronicle file: {} according to retention: {}", file::getAbsolutePath, () -> retention);
        try {
            queue.closeStore(store);
            Files.delete(file.toPath());
            queue.refreshDirectoryListing();
            log.debug(file + " deleted");
        } catch (IOException | SecurityException e) {
            log.warn("Unable to delete Chronicle file: {}, {}", file::getAbsolutePath, e::getMessage);
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

    @Override
    public void onReleased(int cycle, File file) {
        if (queue == null || queue.isClosed() || retention.disable()) {
            return;
        }
        log.debug("Release Chronicle file: {}, cycle: {}", file, cycle);
        if (checkPurge()) {
            purge();
        }
    }

    protected boolean checkPurge() {
        // there is no need to purge more than the cycle length (which is duration in ms)
        if (System.currentTimeMillis() - purgedStamp >= retention.getRollCycle().length()) {
            return true;
        }
        log.debug("Skipping purge already done in within cycle duration: {}", purgedStamp);
        return false;
    }

}

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
package org.nuxeo.ecm.core.work;

import static org.nuxeo.ecm.core.work.StreamWorkManager.KV_NAME;
import static org.nuxeo.ecm.core.work.StreamWorkManager.STATETTL_DEFAULT_VALUE;
import static org.nuxeo.ecm.core.work.StreamWorkManager.STATETTL_KEY;
import static org.nuxeo.ecm.core.work.StreamWorkManager.STATE_SUFFIX;
import static org.nuxeo.ecm.core.work.StreamWorkManager.STORESTATE_KEY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

/**
 * A Stream computation that consumes works.
 *
 * @since 9.3
 */
public class WorkComputation extends AbstractComputation {
    private static final Log log = LogFactory.getLog(WorkComputation.class);

    protected static final int IDS_SIZE = 50;

    protected final CircularFifoBuffer workIds = new CircularFifoBuffer(IDS_SIZE);

    protected final Timer workTimer;

    private final long stateTTL;

    public WorkComputation(String name) {
        super(name, 1, 0);
        MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
        workTimer = registry.timer(MetricRegistry.name("nuxeo", "works", name, "total"));
        stateTTL = Long.parseLong(
                Framework.getService(ConfigurationService.class).getProperty(STATETTL_KEY, STATETTL_DEFAULT_VALUE));
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        Work work = deserialize(record.getData());
        try {
            if (work.isIdempotent() && workIds.contains(work.getId())) {
                log.warn("Duplicate work id: " + work.getId() + " skipping");
            } else {
                boolean storeState = Boolean.parseBoolean(
                        Framework.getService(ConfigurationService.class).getProperty(STORESTATE_KEY));
                if (storeState) {
                    StreamWorkManager.setState(work.getId(), Work.State.RUNNING.toString(), stateTTL);
                }
                new WorkHolder(work).run();
                if (storeState) {
                    KeyValueStore kvStore = Framework.getService(KeyValueService.class).getKeyValueStore(KV_NAME);
                    kvStore.put(work.getId() + STATE_SUFFIX, (String) null);
                }
                workIds.add(work.getId());
            }
            work.cleanUp(true, null);
            context.askForCheckpoint();
        } catch (Exception e) {
            if (ExceptionUtils.hasInterruptedCause(e)) {
                Thread.currentThread().interrupt();
                // propagate the interruption to stop the computation thread
                // thread has been interrupted we don't want to mark the work as completed.
                log.warn(
                        String.format("Work id: %s title: %s, has been interrupted, it will be rescheduled, record: %s",
                                work.getId(), work.getTitle(), record));
            } else {
                // Report an error on the work and continue
                log.error(String.format("Work id: %s title: %s is in error, the work is skipped, record: %s",
                        work.getId(), work.getTitle(), record));
                context.askForCheckpoint();
            }
            // Cleanup should take care of logging error, but better to dup this in debug
            log.debug("Exception during work " + work.getId(), e);
            work.cleanUp(false, e);
        } finally {
            workTimer.update(work.getCompletionTime() - work.getStartTime(), TimeUnit.MILLISECONDS);
        }
    }

    @SuppressWarnings("squid:S2093")
    public static Work deserialize(byte[] data) {
        // TODO: switch to commons-lang3 SerializationUtils
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            return (Work) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception so we cannot use a try-with-resources squid:S2093
            }
        }
    }

    @SuppressWarnings("squid:S2093")
    public static byte[] serialize(Work work) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(work);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception so we cannot use a try-with-resources squid:S2093
            }
        }
    }
}

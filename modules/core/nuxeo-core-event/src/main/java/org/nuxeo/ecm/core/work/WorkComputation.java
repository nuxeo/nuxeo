/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.work.StreamWorkManager.STATETTL_DEFAULT_VALUE;
import static org.nuxeo.ecm.core.work.StreamWorkManager.STATETTL_KEY;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.services.config.ConfigurationService;

import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.Timer;

/**
 * A Stream computation that consumes works.
 *
 * @since 9.3
 */
public class WorkComputation extends AbstractComputation {
    private static final Logger log = LogManager.getLogger(WorkComputation.class);

    protected static final int IDS_SIZE = 50;

    protected final CircularFifoBuffer workIds = new CircularFifoBuffer(IDS_SIZE);

    protected final Timer workTimer;

    protected final long stateTTL;

    protected Work work;

    public WorkComputation(String name) {
        super(name, 1, 0);
        MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
        workTimer = registry.timer(
                MetricName.build("nuxeo.works.queue.timer").tagged("queue", Name.ofUrn(name).getName()));
        stateTTL = Framework.getService(ConfigurationService.class).getLong(STATETTL_KEY, STATETTL_DEFAULT_VALUE);
    }

    @Override
    public void signalStop() {
        if (work != null) {
            work.setWorkInstanceSuspending();
        }
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        work = deserialize(record.getData());
        try {
            if (work.isCoalescing() && WorkStateHelper.getLastOffset(work.getId()) > context.getLastOffset().offset()) {
                log.debug("Skipping duplicate of coalescing work id: " + work.getId() + " " + work);
            } else if (work.isIdempotent() && workIds.contains(work.getId())) {
                log.debug("Skipping duplicate of idempotent work id: " + work.getId());
            } else {
                boolean storeState = Framework.getService(ConfigurationService.class).isBooleanTrue(STORESTATE_KEY);
                if (storeState) {
                    if (WorkStateHelper.getState(work.getId()) != Work.State.SCHEDULED) {
                        log.warn("work has been canceled, saving and returning");
                        context.askForCheckpoint();
                        return;
                    }
                    WorkStateHelper.setState(work.getId(), Work.State.RUNNING, stateTTL);
                }
                // The running state is needed to activate the DLQ mechanism
                work.setWorkInstanceState(Work.State.RUNNING);
                new WorkHolder(work).run();
                // if the same work id has not been scheduled again, set the state to null for 'completed'
                if (storeState && WorkStateHelper.getState(work.getId()) == Work.State.RUNNING) {
                    WorkStateHelper.setState(work.getId(), null, stateTTL);
                }
                workIds.add(work.getId());
            }
            work.cleanUp(true, null);
            if (!work.isWorkInstanceSuspended()) {
                context.askForCheckpoint();
            }
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
                log.error(String.format(
                        "Skip Work in failure: id: %s, title: %s, offset: %s, record: %s, thread: %s", work.getId(),
                        work.getTitle(), context.getLastOffset(), record, Thread.currentThread().getName()));
                context.askForCheckpoint();
            }
            // Cleanup should take care of logging error except if exception comes from the cleanup
            log.debug("Exception during work " + work.getId(), e);
            // Try to cleanup after an exception, if exception comes from the previous cleanup it is a duplicate cleanup
            cleanupWorkInFailure(work, e);
        } finally {
            workTimer.update(work.getCompletionTime() - work.getStartTime(), TimeUnit.MILLISECONDS);
            work = null;
        }
    }

    protected void cleanupWorkInFailure(Work work, Exception exception) {
        try {
            work.cleanUp(false, exception);
        } catch (Exception e) {
            log.error("Error during cleanup work: " + work.getId(), e);
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

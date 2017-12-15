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
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.metrics.MetricsService;

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

    public WorkComputation(String name) {
        super(name, 1, 0);
        MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
        workTimer = registry.timer(MetricRegistry.name("nuxeo", "works", name, "total"));
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        Work work = deserialize(record.data);
        try {
            if (work.isIdempotent() && workIds.contains(work.getId())) {
                log.warn("Duplicate work id: " + work.getId() + " skipping");
            } else {
                new WorkHolder(work).run();
                workIds.add(work.getId());
            }
        } catch (Exception e) {
            // TODO: check what to catch exactly we don't want to kill the computation on error
            log.warn(String.format("Work id: %s title: %s, raise an exception, work is marked as completed",
                    work.getId(), work.getTitle()), e);
        } finally {
            work.cleanUp(true, null);
            workTimer.update(work.getCompletionTime() - work.getStartTime(), TimeUnit.MILLISECONDS);
            context.askForCheckpoint();
        }
    }

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
                // ignore close exception
            }
        }
    }

    public static byte[] serialize(Work work) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(work);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            System.out.println("Error " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }
}

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
package org.nuxeo.lib.stream.tests.computation;

import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;

/**
 * Source computation that produces random records. Source computation must take care of submitting ordered watermark
 * The target timestamp is used to build the watermark of the last record.
 *
 * @since 9.3
 */
public class ComputationSource extends AbstractComputation {
    private static final Log log = LogFactory.getLog(ComputationSource.class);

    protected final int records;

    protected final int batchSize;

    protected int generated = 0;

    protected long targetTimestamp;

    public ComputationSource(String name) {
        this(name, 1, 10, 3, 0);
    }

    public ComputationSource(String name, int outputs, int records, int batchSize, long targetTimestamp) {
        this(name, outputs, records, batchSize, targetTimestamp, false);
    }

    public ComputationSource(String name, int outputs, int records, int batchSize, long targetTimestamp, boolean uniq) {
        super(name, uniq ? 1 : 0, outputs);
        if (outputs <= 0) {
            throw new IllegalArgumentException("Cannot produce records without output streams");
        }
        this.records = records;
        this.batchSize = batchSize;
        this.targetTimestamp = targetTimestamp;
    }

    @Override
    public void init(ComputationContext context) {
        context.setTimer("generate", System.currentTimeMillis());
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        // source computation has no input
    }

    @Override
    public void processTimer(ComputationContext context, String key, long time) {
        if ("generate".equals(key)) {
            int endOfBatch = Math.min(generated + batchSize, records);
            do {
                generated += 1;
                metadata.outputStreams().forEach(o -> context.produceRecord(o, getRandomRecord()));
                if (generated % 100 == 0) {
                    log.debug("Generate record: " + generated + " wm " + getWatermark());
                }
            } while (generated < endOfBatch);
            // try {
            // Thread.sleep(499);
            // } catch (InterruptedException e) {
            // Thread.currentThread().interrupt();
            // throw new RuntimeException(e);
            // }
            if (generated < records) {
                context.setTimer("generate", System.currentTimeMillis());
                context.setSourceLowWatermark(getWatermark());
            } else {
                log.info("Generate record terminated: " + generated + " last wm " + getWatermark());
                context.setSourceLowWatermark(Watermark.completedOf(Watermark.ofTimestamp(targetTimestamp)).getValue());
            }
            context.askForCheckpoint();
        }
    }

    protected long getWatermark() {
        // return watermark that increment up to target
        return Watermark.ofTimestamp(targetTimestamp - (records - generated)).getValue();
    }

    protected Record getRandomRecord() {
        String msg = "data from " + metadata.name() + " msg " + generated;
        Record ret;
        try {
            ret = Record.of("key" + generated, msg.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // should never happen
            throw new RuntimeException(e);
        }
        ret.setWatermark(getWatermark());
        return ret;
    }
}

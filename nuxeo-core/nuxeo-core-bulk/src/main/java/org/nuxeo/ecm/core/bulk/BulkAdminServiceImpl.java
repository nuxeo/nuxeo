/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.BULK_LOG_MANAGER_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.COMMAND_STREAM;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.DONE_STREAM;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.RECORD_CODEC;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.bulk.computation.BulkScrollerComputation;
import org.nuxeo.ecm.core.bulk.computation.BulkStatusComputation;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.log.LogStreamProcessor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.stream.StreamService;

/**
 * @since 10.3
 */
public class BulkAdminServiceImpl implements BulkAdminService {

    public static final String SCROLLER_NAME = "scroller";

    public static final String STATUS_NAME = "status";

    public static final String BULK_SCROLLER_CONCURRENCY_PROPERTY = "nuxeo.core.bulk.scroller.concurrency";

    public static final String BULK_STATUS_CONCURRENCY_PROPERTY = "nuxeo.core.bulk.status.concurrency";

    public static final String BULK_DONE_CONCURRENCY_PROPERTY = "nuxeo.core.bulk.done.concurrency";

    public static final String BULK_SCROLL_SIZE_PROPERTY = "nuxeo.core.bulk.scroller.scroll.size";

    public static final String BULK_SCROLL_KEEP_ALIVE_PROPERTY = "nuxeo.core.bulk.scroller.scroll.keepAliveSeconds";

    public static final String BULK_SCROLL_PRODUCE_IMMEDIATE_PROPERTY = "nuxeo.core.bulk.scroller.produceImmediate";

    public static final String DEFAULT_SCROLLER_CONCURRENCY = "1";

    public static final String DEFAULT_STATUS_CONCURRENCY = "1";

    public static final String DEFAULT_DONE_CONCURRENCY = "1";

    public static final String DEFAULT_SCROLL_SIZE = "100";

    public static final String DEFAULT_SCROLL_KEEP_ALIVE = "60";

    public static final String DEFAULT_SCROLL_PRODUCE_IMMEDIATE = "false";

    public static final Duration STOP_DURATION = Duration.ofSeconds(1);

    protected final Map<String, BulkActionDescriptor> descriptors;

    protected final List<String> actions;

    protected StreamProcessor streamProcessor;

    public BulkAdminServiceImpl(List<BulkActionDescriptor> descriptorsList) {
        this.actions = descriptorsList.stream().map(Descriptor::getId).collect(Collectors.toList());
        this.descriptors = new HashMap<>(descriptorsList.size());
        descriptorsList.forEach(descriptor -> descriptors.put(descriptor.name, descriptor));
    }

    protected void initProcessor() {
        StreamService service = Framework.getService(StreamService.class);
        ConfigurationService confService = Framework.getService(ConfigurationService.class);
        streamProcessor = new LogStreamProcessor(service.getLogManager(BULK_LOG_MANAGER_NAME));
        CodecService codecService = Framework.getService(CodecService.class);
        Codec<Record> codec = codecService.getCodec(RECORD_CODEC, Record.class);
        // we don't set any partitioning because it is already defined by logConfig contribution
        Settings settings = new Settings(1, 1, codec);
        settings.setConcurrency(SCROLLER_NAME, Integer.parseInt(
                confService.getProperty(BULK_SCROLLER_CONCURRENCY_PROPERTY, DEFAULT_SCROLLER_CONCURRENCY)));
        settings.setConcurrency(STATUS_NAME, Integer.parseInt(
                confService.getProperty(BULK_STATUS_CONCURRENCY_PROPERTY, DEFAULT_STATUS_CONCURRENCY)));
        settings.setConcurrency(STATUS_NAME, Integer.parseInt(
                confService.getProperty(BULK_DONE_CONCURRENCY_PROPERTY, DEFAULT_DONE_CONCURRENCY)));
        int scrollSize = Integer.parseInt(confService.getProperty(BULK_SCROLL_SIZE_PROPERTY, DEFAULT_SCROLL_SIZE));
        int scrollKeepAlive = Integer.parseInt(
                confService.getProperty(BULK_SCROLL_KEEP_ALIVE_PROPERTY, DEFAULT_SCROLL_KEEP_ALIVE));
        boolean scrollProduceImmediate = Boolean.parseBoolean(
                confService.getProperty(BULK_SCROLL_PRODUCE_IMMEDIATE_PROPERTY, DEFAULT_SCROLL_PRODUCE_IMMEDIATE));
        streamProcessor.init(getTopology(scrollSize, scrollKeepAlive, scrollProduceImmediate), settings);
    }

    protected Topology getTopology(int scrollBatchSize, int scrollKeepAlive, boolean scrollProduceImmediate) {
        List<String> actions = getActions();
        List<String> mapping = new ArrayList<>();
        mapping.add(INPUT_1 + ":" + COMMAND_STREAM);
        int i = 1;
        for (String action : actions) {
            mapping.add(String.format("o%s:%s", i, action));
            i++;
        }
        mapping.add(String.format("o%s:%s", i, STATUS_STREAM));
        return Topology.builder()
                       .addComputation( //
                               () -> new BulkScrollerComputation(SCROLLER_NAME, actions.size() + 1, scrollBatchSize,
                                       scrollKeepAlive, scrollProduceImmediate), //
                               mapping)
                       .addComputation(() -> new BulkStatusComputation(STATUS_NAME),
                               Arrays.asList(INPUT_1 + ":" + STATUS_STREAM, //
                                       OUTPUT_1 + ":" + DONE_STREAM))
                       .build();
    }

    @Override
    public List<String> getActions() {
        return actions;
    }

    @Override
    public int getBucketSize(String action) {
        return descriptors.get(action).getBucketSize();
    }

    @Override
    public int getBatchSize(String action) {
        return descriptors.get(action).getBatchSize();
    }

    public void afterStart() {
        initProcessor();
        streamProcessor.start();
    }

    public void beforeStop() {
        if (streamProcessor != null) {
            streamProcessor.stop(STOP_DURATION);
        }
    }
}

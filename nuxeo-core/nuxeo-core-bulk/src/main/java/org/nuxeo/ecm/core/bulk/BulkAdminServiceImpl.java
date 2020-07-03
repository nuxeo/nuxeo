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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.bulk.computation.BulkScrollerComputation;
import org.nuxeo.ecm.core.bulk.computation.BulkStatusComputation;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.log.LogStreamProcessor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.stream.StreamService;

import net.jodah.failsafe.RetryPolicy;

/**
 * @since 10.3
 */
public class BulkAdminServiceImpl implements BulkAdminService {

    public static final String SCROLLER_NAME = "scroller";

    public static final String STATUS_NAME = "status";

    public static final String BULK_SCROLLER_CONCURRENCY_PROPERTY = "nuxeo.core.bulk.scroller.concurrency";

    public static final String BULK_STATUS_CONCURRENCY_PROPERTY = "nuxeo.core.bulk.status.concurrency";

    public static final String BULK_STATUS_CONTINUE_ON_FAILURE_PROPERTY = "nuxeo.core.bulk.status.continueOnFailure";

    public static final String BULK_STATUS_MAX_RETRIES_PROPERTY = "nuxeo.core.bulk.status.maxRetries";

    public static final String BULK_STATUS_DELAY_PROPERTY = "nuxeo.core.bulk.status.delayMillis";

    public static final String BULK_STATUS_MAX_DELAY_PROPERTY = "nuxeo.core.bulk.status.maxDelayMillis";

    public static final String BULK_SCROLL_SIZE_PROPERTY = "nuxeo.core.bulk.scroller.scroll.size";

    public static final String BULK_SCROLL_KEEP_ALIVE_PROPERTY = "nuxeo.core.bulk.scroller.scroll.keepAliveSeconds";

    public static final String BULK_SCROLL_PRODUCE_IMMEDIATE_PROPERTY = "nuxeo.core.bulk.scroller.produceImmediate";

    public static final String BULK_SCROLL_CONTINUE_ON_FAILURE_PROPERTY = "nuxeo.core.bulk.scroller.continueOnFailure";

    public static final String DEFAULT_STATUS_CONCURRENCY = "1";

    public static final String DEFAULT_STATUS_MAX_RETRIES = "3";

    public static final String DEFAULT_STATUS_DELAY_MILLIS = "500";

    public static final String DEFAULT_STATUS_MAX_DELAY_MILLIS = "10000";

    public static final String DEFAULT_SCROLLER_CONCURRENCY = "1";

    public static final String DEFAULT_SCROLL_SIZE = "100";

    public static final String DEFAULT_SCROLL_KEEP_ALIVE = "60";

    public static final String DEFAULT_SCROLL_PRODUCE_IMMEDIATE = "false";

    // @since 11.2
    public static final String BULK_SCROLL_TRANSACTION_TIMEOUT_PROPERTY = "nuxeo.core.bulk.scroller.transactionTimeout";

    // @since 11.2
    public static final Duration DEFAULT_SCROLL_TRANSACTION_TIMEOUT = Duration.ofDays(2);

    public static final Duration STOP_DURATION = Duration.ofSeconds(1);

    protected final Map<String, BulkActionDescriptor> descriptors;

    protected final List<String> actions;

    protected StreamProcessor streamProcessor;

    protected Map<String, BulkActionValidation> actionValidations;

    public BulkAdminServiceImpl(List<BulkActionDescriptor> descriptorsList) {
        this.actions = descriptorsList.stream().map(Descriptor::getId).collect(Collectors.toList());
        this.descriptors = new HashMap<>(descriptorsList.size());
        descriptorsList.forEach(descriptor -> descriptors.put(descriptor.name, descriptor));
        actionValidations = descriptorsList.stream().collect(HashMap::new,
                (map, desc) -> map.put(desc.name, desc.validationClass != null ? desc.newValidationInstance() : null),
                 HashMap::putAll);
    }

    protected void initProcessor() {
        StreamService service = Framework.getService(StreamService.class);
        ConfigurationService confService = Framework.getService(ConfigurationService.class);
        StreamManager streamManager = service.getStreamManager(BULK_LOG_MANAGER_NAME);

        CodecService codecService = Framework.getService(CodecService.class);
        Codec<Record> codec = codecService.getCodec(RECORD_CODEC, Record.class);
        // we don't set any partitioning because it is already defined by logConfig contribution
        Settings settings = new Settings(1, 1, codec);
        settings.setConcurrency(SCROLLER_NAME, Integer.parseInt(
                confService.getProperty(BULK_SCROLLER_CONCURRENCY_PROPERTY, DEFAULT_SCROLLER_CONCURRENCY)));
        settings.setConcurrency(STATUS_NAME, Integer.parseInt(
                confService.getProperty(BULK_STATUS_CONCURRENCY_PROPERTY, DEFAULT_STATUS_CONCURRENCY)));
        // we don't want any retry on scroller this creates duplicates
        ComputationPolicy scrollerPolicy = new ComputationPolicyBuilder().continueOnFailure(
                confService.isBooleanPropertyTrue(BULK_SCROLL_CONTINUE_ON_FAILURE_PROPERTY))
                                                                         .retryPolicy(ComputationPolicy.NO_RETRY)
                                                                         .build();
        settings.setPolicy(SCROLLER_NAME, scrollerPolicy);
        // status policy is configurable
        RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(
                Integer.parseInt(confService.getProperty(BULK_STATUS_MAX_RETRIES_PROPERTY, DEFAULT_STATUS_MAX_RETRIES)))
                                                   .withBackoff(
                                                           Integer.parseInt(
                                                                   confService.getProperty(BULK_STATUS_DELAY_PROPERTY,
                                                                           DEFAULT_STATUS_DELAY_MILLIS)),
                                                           Integer.parseInt(confService.getProperty(
                                                                   BULK_STATUS_MAX_DELAY_PROPERTY,
                                                                   DEFAULT_STATUS_MAX_DELAY_MILLIS)),
                                                           TimeUnit.MILLISECONDS);
        ComputationPolicy statusPolicy = new ComputationPolicyBuilder().continueOnFailure(
                confService.isBooleanPropertyTrue(BULK_STATUS_CONTINUE_ON_FAILURE_PROPERTY))
                                                                       .retryPolicy(retryPolicy)
                                                                       .build();
        settings.setPolicy(SCROLLER_NAME, statusPolicy);
        int scrollSize = Integer.parseInt(confService.getProperty(BULK_SCROLL_SIZE_PROPERTY, DEFAULT_SCROLL_SIZE));
        int scrollKeepAlive = Integer.parseInt(
                confService.getProperty(BULK_SCROLL_KEEP_ALIVE_PROPERTY, DEFAULT_SCROLL_KEEP_ALIVE));
        boolean scrollProduceImmediate = Boolean.parseBoolean(
                confService.getProperty(BULK_SCROLL_PRODUCE_IMMEDIATE_PROPERTY, DEFAULT_SCROLL_PRODUCE_IMMEDIATE));
        Duration transactionTimeout = confService.getDuration(BULK_SCROLL_TRANSACTION_TIMEOUT_PROPERTY,
                DEFAULT_SCROLL_TRANSACTION_TIMEOUT);
        streamProcessor = streamManager.registerAndCreateProcessor("bulk", getTopology(scrollSize, scrollKeepAlive, transactionTimeout, scrollProduceImmediate), settings);
    }

    protected Topology getTopology(int scrollBatchSize, int scrollKeepAlive, Duration transactionTimeout, boolean scrollProduceImmediate) {
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
                                       scrollKeepAlive, transactionTimeout, scrollProduceImmediate), //
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

    @Override
    public String getDefaultScroller(String action) {
        return descriptors.get(action).getDefaultScroller();
    }

    @Override
    public boolean isHttpEnabled(String actionId) {
        return descriptors.get(actionId).httpEnabled;
    }

    @Override
    public boolean isSequentialCommands(String actionId) {
        return descriptors.get(actionId).sequentialCommands;
    }

    @Override
    public BulkActionValidation getActionValidation(String action) {
        return actionValidations.get(action);
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

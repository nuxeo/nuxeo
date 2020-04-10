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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.runtime.stream.StreamService;

/**
 * @since 10.3
 */
public class BulkAdminServiceImpl implements BulkAdminService {

    public static final String SCROLLER_NAME = "bulk/scroller";

    public static final String STATUS_NAME = "bulk/status";

    public static final String BULK_SERVICE_PROCESSOR_NAME = "bulkServiceProcessor";

    public static final String BULK_SCROLL_SIZE_PROPERTY = "nuxeo.core.bulk.scroller.scroll.size";

    public static final String BULK_SCROLL_KEEP_ALIVE_PROPERTY = "nuxeo.core.bulk.scroller.scroll.keepAliveSeconds";

    public static final String BULK_SCROLL_PRODUCE_IMMEDIATE_PROPERTY = "nuxeo.core.bulk.scroller.produceImmediate";


    public static final int DEFAULT_SCROLL_SIZE = 100;

    public static final int DEFAULT_SCROLL_KEEP_ALIVE = 60;

    public static final Duration STOP_DURATION = Duration.ofSeconds(1);

    protected final Map<String, BulkActionDescriptor> descriptors;

    protected final List<String> actions;

    protected StreamProcessor streamProcessor;

    protected Map<String, BulkActionValidation> actionValidations;

    public BulkAdminServiceImpl(List<BulkActionDescriptor> descriptorsList) {
        this.actions = descriptorsList.stream().map(Descriptor::getId).collect(Collectors.toList());
        this.descriptors = new HashMap<>(descriptorsList.size());
        descriptorsList.forEach(descriptor -> descriptors.put(descriptor.name, descriptor));
        actionValidations = descriptorsList.stream()
                                           .collect(HashMap::new, (map, desc) -> map.put(desc.name,
                                                   desc.validationClass != null ? desc.newValidationInstance() : null),
                                                   HashMap::putAll);
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
    public String getInputStream(String action) {
        return descriptors.get(action).getInputStream();
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
        StreamManager manager = Framework.getService(StreamService.class).getStreamManager();
        streamProcessor = manager.createStreamProcessor(BULK_SERVICE_PROCESSOR_NAME);
        streamProcessor.start();
    }

    public void beforeStop() {
        if (streamProcessor != null) {
            streamProcessor.stop(STOP_DURATION);
        }
    }

}

/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.bulk;

import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.BULK_SCROLL_KEEP_ALIVE_PROPERTY;
import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.BULK_SCROLL_PRODUCE_IMMEDIATE_PROPERTY;
import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.BULK_SCROLL_SIZE_PROPERTY;
import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.DEFAULT_SCROLL_KEEP_ALIVE;
import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.DEFAULT_SCROLL_SIZE;
import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.SCROLLER_NAME;
import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.STATUS_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.COMMAND_STREAM;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.DONE_STREAM;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.bulk.computation.BulkScrollerComputation;
import org.nuxeo.ecm.core.bulk.computation.BulkStatusComputation;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 11.1
 */
public class BulkServiceProcessor implements StreamProcessorTopology {

    @Override
    public Topology getTopology(Map<String, String> options) {
        List<String> mapping = new ArrayList<>();
        mapping.add(INPUT_1 + ":" + COMMAND_STREAM);
        BulkAdminService actionService = Framework.getService(BulkAdminService.class);
        List<String> actions = actionService.getActions();
        int i = 1;
        for (String action : actions) {
            mapping.add(String.format("o%s:%s", i, actionService.getInputStream(action)));
            i++;
        }
        mapping.add(String.format("o%s:%s", i, STATUS_STREAM));
        ConfigurationService confService = Framework.getService(ConfigurationService.class);
        int scrollBatchSize = confService.getInteger(BULK_SCROLL_SIZE_PROPERTY, DEFAULT_SCROLL_SIZE);
        int scrollKeepAlive = confService.getInteger(BULK_SCROLL_KEEP_ALIVE_PROPERTY, DEFAULT_SCROLL_KEEP_ALIVE);
        boolean scrollProduceImmediate = confService.isBooleanTrue(BULK_SCROLL_PRODUCE_IMMEDIATE_PROPERTY);
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
}

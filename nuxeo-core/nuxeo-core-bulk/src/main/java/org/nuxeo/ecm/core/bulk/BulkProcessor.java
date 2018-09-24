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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.DOCUMENTSET_ACTION_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.bulk.computation.BulkCounterComputation;
import org.nuxeo.ecm.core.bulk.computation.BulkScrollerComputation;
import org.nuxeo.ecm.core.bulk.computation.BulkStatusComputation;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Stream Processor that runs the Bulk Action service. It reads commands and uses a scroller to materialize the document
 * set into buckets of document ids. The action is a distinct processor runs on document set and produces counter to
 * signal his progress. A counter computation aggregates the counter messages and produce command status message that
 * are written to a key value store.
 *
 * @since 10.2
 */
public class BulkProcessor implements StreamProcessorTopology {

    private static final Log log = LogFactory.getLog(BulkProcessor.class);

    public static final String COUNTER_ACTION_NAME = "counter";

    public static final String KVWRITER_ACTION_NAME = "keyValueWriter";

    public static final String SCROLL_BATCH_SIZE_OPT = "scrollBatchSize";

    public static final String SCROLL_KEEP_ALIVE_SECONDS_OPT = "scrollKeepAlive";

    public static final String BUCKET_SIZE_OPT = "bucketSize";

    public static final String COUNTER_THRESHOLD_MS_OPT = "counterThresholdMs";

    public static final int DEFAULT_SCROLL_BATCH_SIZE = 100;

    public static final int DEFAULT_SCROLL_KEEPALIVE_SECONDS = 60;

    public static final int DEFAULT_BUCKET_SIZE = 50;

    public static final int DEFAULT_COUNTER_THRESHOLD_MS = 30000;

    @Override
    public Topology getTopology(Map<String, String> options) {
        // retrieve options
        int scrollBatchSize = getOptionAsInteger(options, SCROLL_BATCH_SIZE_OPT, DEFAULT_SCROLL_BATCH_SIZE);
        int scrollKeepAliveSeconds = getOptionAsInteger(options, SCROLL_KEEP_ALIVE_SECONDS_OPT,
                DEFAULT_SCROLL_KEEPALIVE_SECONDS);
        int bucketSize = getOptionAsInteger(options, BUCKET_SIZE_OPT, DEFAULT_BUCKET_SIZE);
        int counterThresholdMs = getOptionAsInteger(options, COUNTER_THRESHOLD_MS_OPT, DEFAULT_COUNTER_THRESHOLD_MS);

        // retrieve bulk actions to deduce output streams
        BulkAdminService service = Framework.getService(BulkAdminService.class);
        List<String> actions = service.getActions();
        List<String> mapping = new ArrayList<>();
        mapping.add("i1:" + DOCUMENTSET_ACTION_NAME);
        int i = 1;
        for (String action : actions) {
            mapping.add(String.format("o%s:%s", i, action));
            i++;
        }
        mapping.add(String.format("o%s:%s", i, KVWRITER_ACTION_NAME));

        return Topology.builder()
                       .addComputation( //
                               () -> new BulkScrollerComputation(DOCUMENTSET_ACTION_NAME, mapping.size(),
                                       scrollBatchSize, scrollKeepAliveSeconds, bucketSize), //
                               mapping)
                       .addComputation(() -> new BulkCounterComputation(COUNTER_ACTION_NAME, counterThresholdMs),
                               Arrays.asList("i1:" + COUNTER_ACTION_NAME, "o1:" + KVWRITER_ACTION_NAME))
                       .addComputation(() -> new BulkStatusComputation(KVWRITER_ACTION_NAME),
                               Collections.singletonList("i1:" + KVWRITER_ACTION_NAME))
                       .build();
    }

    // TODO copied from StreamAuditWriter - where can we put that ?
    protected int getOptionAsInteger(Map<String, String> options, String option, int defaultValue) {
        String value = options.get(option);
        return value == null ? defaultValue : Integer.parseInt(value);
    }
}

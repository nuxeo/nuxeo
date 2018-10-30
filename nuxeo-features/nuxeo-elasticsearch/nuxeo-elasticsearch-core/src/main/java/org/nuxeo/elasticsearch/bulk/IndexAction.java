/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.bulk;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.DONE_STREAM;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * A Nuxeo Bulk Action to index documents. it decouples the document extraction to build the elasticsearch request and
 * the indexing.
 *
 * @since 10.3
 */
public class IndexAction implements StreamProcessorTopology {
    public static final String ACTION_NAME = "index";

    public static final String CONTINUE_ON_FAILURE = "continueOnFailure";

    public static final String ES_BULK_SIZE_OPTION = "esBulkSizeBytes";

    public static final int DEFAULT_BULK_SIZE = 5_242_880;

    public static final String ES_BULK_ACTION_OPTION = "esBulkActions";

    public static final int DEFAULT_BULK_ACTIONS = 1_000;

    public static final String BULK_FLUSH_INTERVAL = "flushIntervalSeconds";

    public static final int DEFAULT_BULK_INTERVAL = 10;

    public static final String INDEX_UPDATE_ALIAS_PARAM = "updateAlias";

    public static final String REFRESH_INDEX_PARAM = "refresh";

    @Override
    public Topology getTopology(Map<String, String> options) {
        boolean continueOnFailure = getOptionAsBoolean(options, CONTINUE_ON_FAILURE, false);
        int esBulkSize = getOptionAsInteger(options, ES_BULK_SIZE_OPTION, DEFAULT_BULK_SIZE);
        int esBulkActions = getOptionAsInteger(options, ES_BULK_ACTION_OPTION, DEFAULT_BULK_ACTIONS);
        int esBulkFlushInterval = getOptionAsInteger(options, BULK_FLUSH_INTERVAL, DEFAULT_BULK_INTERVAL);
        return Topology.builder()
                       .addComputation(() -> new IndexRequestComputation(continueOnFailure),
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, //
                               OUTPUT_1 + ":" + BulkIndexComputation.NAME))
                       .addComputation(
                               () -> new BulkIndexComputation(
                                       continueOnFailure, esBulkSize, esBulkActions, esBulkFlushInterval),
                               Arrays.asList(INPUT_1 + ":" + BulkIndexComputation.NAME, //
                               OUTPUT_1 + ":" + STATUS_STREAM))
                       .addComputation(() -> new IndexCompletionComputation(continueOnFailure),
                               Collections.singletonList(INPUT_1 + ":" + DONE_STREAM))
                       .build();

    }

    public static boolean getOptionAsBoolean(Map<String, String> options, String option, boolean defaultValue) {
        String value = options.get(option);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    public static int getOptionAsInteger(Map<String, String> options, String option, int defaultValue) {
        String value = options.get(option);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

}

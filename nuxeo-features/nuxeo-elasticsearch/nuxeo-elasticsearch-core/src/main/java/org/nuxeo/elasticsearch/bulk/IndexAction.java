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
import java.util.concurrent.TimeUnit;

import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

import net.jodah.failsafe.RetryPolicy;

/**
 * A Nuxeo Bulk Action to index documents. it decouples the document extraction to build the elasticsearch request and
 * the indexing.
 *
 * @since 10.3
 */
public class IndexAction implements StreamProcessorTopology {
    public static final String ACTION_NAME = "index";

    public static final String CONTINUE_ON_FAILURE_OPTION = "continueOnFailure";

    public static final String MAX_RETRIES_OPTION = "maxRetries";

    public static final int MAX_RETRIES_DEFAULT = 3;

    public static final String ES_BULK_SIZE_OPTION = "esBulkSizeBytes";

    public static final int ES_BULK_SIZE_DEFAULT = 5_242_880;

    public static final String ES_BULK_ACTION_OPTION = "esBulkActions";

    public static final int ES_BULK_ACTION_DEFAULT = 1_000;

    public static final String BULK_FLUSH_INTERVAL_OPTION = "flushIntervalSeconds";

    public static final int BULK_FLUSH_INTERVAL_DEFAULT = 10;

    public static final String BACKOFF_DELAY_OPTION = "backoffDelayMs";

    public static final int BACKOFF_DELAY_DEFAULT = 1_000;

    public static final String BACKOFF_MAX_DELAY_OPTION = "backoffMaxDelayMs";

    public static final int BACKOFF_MAX_DELAY_DEFAULT = 10_000;

    public static final String INDEX_UPDATE_ALIAS_PARAM = "updateAlias";

    public static final String REFRESH_INDEX_PARAM = "refresh";


    @Override
    public Topology getTopology(Map<String, String> options) {
        int esBulkSize = getOptionAsInteger(options, ES_BULK_SIZE_OPTION, ES_BULK_SIZE_DEFAULT);
        int esBulkActions = getOptionAsInteger(options, ES_BULK_ACTION_OPTION, ES_BULK_ACTION_DEFAULT);
        int esBulkFlushInterval = getOptionAsInteger(options, BULK_FLUSH_INTERVAL_OPTION, BULK_FLUSH_INTERVAL_DEFAULT);
        boolean continueOnFailure = getOptionAsBoolean(options, CONTINUE_ON_FAILURE_OPTION, false);
        ComputationPolicy policy = getPolicy(options);
        return Topology.builder()
                       .addComputation(() -> new IndexRequestComputation(policy),
                               Arrays.asList(INPUT_1 + ":" + ACTION_NAME, //
                               OUTPUT_1 + ":" + BulkIndexComputation.NAME))
                       .addComputation(
                               () -> new BulkIndexComputation(
                                       policy, esBulkSize, esBulkActions, esBulkFlushInterval),
                               Arrays.asList(INPUT_1 + ":" + BulkIndexComputation.NAME, //
                               OUTPUT_1 + ":" + STATUS_STREAM))
                       .addComputation(() -> new IndexCompletionComputation(policy),
                               Collections.singletonList(INPUT_1 + ":" + DONE_STREAM))
                       .build();

    }

    protected ComputationPolicy getPolicy(Map<String, String> options) {
        boolean continueOnFailure = getOptionAsBoolean(options, CONTINUE_ON_FAILURE_OPTION, false);
        int maxRetries = getOptionAsInteger(options, MAX_RETRIES_OPTION, MAX_RETRIES_DEFAULT);
        int backoffDelay = getOptionAsInteger(options, BACKOFF_DELAY_OPTION, BACKOFF_DELAY_DEFAULT);
        int backoffMaxDelay = getOptionAsInteger(options, BACKOFF_MAX_DELAY_OPTION, BACKOFF_MAX_DELAY_DEFAULT);
        RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(maxRetries)
                                                   .withBackoff(backoffDelay, backoffMaxDelay, TimeUnit.MILLISECONDS);
        return new ComputationPolicyBuilder().continueOnFailure(continueOnFailure).retryPolicy(retryPolicy).build();
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

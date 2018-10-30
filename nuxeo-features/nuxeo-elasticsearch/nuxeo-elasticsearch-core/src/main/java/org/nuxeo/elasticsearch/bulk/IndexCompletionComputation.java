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

import static org.nuxeo.elasticsearch.bulk.IndexAction.INDEX_UPDATE_ALIAS_PARAM;
import static org.nuxeo.elasticsearch.bulk.IndexAction.REFRESH_INDEX_PARAM;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;

/**
 * On indexing completion, do extra tasks like refresh or update index alias.
 *
 * @since 10.3
 */
public class IndexCompletionComputation extends AbstractComputation {
    private static final Log log = LogFactory.getLog(IndexCompletionComputation.class);

    public static final String NAME = "indexCompletion";

    protected final boolean continueOnFailure;

    protected Codec<BulkStatus> codec;

    public IndexCompletionComputation(boolean continueOnFailure) {
        super(NAME, 1, 0);
        this.continueOnFailure = continueOnFailure;
    }

    @Override
    public void init(ComputationContext context) {
        super.init(context);
        this.codec = BulkCodecs.getStatusCodec();
    }

    @Override
    public void processRecord(ComputationContext context, String inputStream, Record record) {
        BulkStatus status = codec.decode(record.getData());
        if (IndexAction.ACTION_NAME.equals(status.getAction())
                && BulkStatus.State.COMPLETED.equals(status.getState())) {
            logIndexing(status);
            BulkService bulkService = Framework.getService(BulkService.class);
            BulkCommand command = bulkService.getCommand(status.getCommandId());
            refreshIndexIfNeeded(command);
            updateAliasIfNeeded(command);
        }
        context.askForCheckpoint();
    }

    protected void refreshIndexIfNeeded(BulkCommand command) {
        Boolean refresh = command.getParam(REFRESH_INDEX_PARAM);
        if (Boolean.TRUE.equals(refresh)) {
            log.warn("Refresh index requested by command: " + command.getId());
            ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
            esa.refreshRepositoryIndex(command.getRepository());
        }
    }

    protected void updateAliasIfNeeded(BulkCommand command) {
        Boolean updateAlias = command.getParam(INDEX_UPDATE_ALIAS_PARAM);
        if (Boolean.TRUE.equals(updateAlias)) {
            log.warn("Update alias requested by command: " + command.getId());
            ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
            esa.syncSearchAndWriteAlias(esa.getIndexNameForRepository(command.getRepository()));
        }
    }

    protected void logIndexing(BulkStatus status) {
        long elapsed = status.getCompletedTime().toEpochMilli() - status.getSubmitTime().toEpochMilli();
        long wait = status.getScrollStartTime().toEpochMilli() - status.getSubmitTime().toEpochMilli();
        long scroll = status.getScrollEndTime().toEpochMilli() - status.getScrollStartTime().toEpochMilli();
        double rate = 1000.0 * status.getTotal() / (elapsed);
        log.warn(String.format(
                "Index command: %s completed: %d docs in %.2fs (wait: %.2fs, scroll: %.2fs) rate: %.2f docs/s",
                status.getCommandId(), status.getTotal(), elapsed / 1000.0, wait / 1000.0, scroll / 1000.0, rate));
    }

}

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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Abstract Work to process a list of documents.
 *
 * @since 9.10
 */
public abstract class BatchProcessorWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(BatchProcessorWork.class);

    public BatchProcessorWork(String repositoryName, List<String> docIds, String originatingUsername) {
        setDocuments(repositoryName, docIds);
        setOriginatingUsername(originatingUsername);
    }

    @Override
    public int getRetryCount() {
        // even read-only threads may encounter concurrent update exceptions when trying to read
        // a previously deleted complex property due to read committed semantics (see NXP-17384)
        return 1;
    }

    /**
     * The batch size to use.
     */
    public abstract int getBatchSize();

    @Override
    public void work() {
        int size = docIds.size();
        int batchSize = getBatchSize();
        if (log.isDebugEnabled()) {
            log.debug(getTitle() + ": Starting processing: " + size + " documents with batch size: " + batchSize);
        }
        openSystemSession();
        setProgress(new Progress(0, size));
        setStatus("Processing");

        for (int start = 0; start < size; start += batchSize) {
            int end = start + batchSize;
            if (end > size) {
                end = size;
            }
            List<String> batch = docIds.subList(start, end);
            // process the batch
            processBatch(batch);
            setProgress(new Progress(end, size));
            // next batch
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        if (log.isDebugEnabled()) {
            log.debug(getTitle() + ": Finished processing for batch of size:" + size);
        }
        setStatus("Done");
    }

    public abstract void processBatch(List<String> docIds);

}

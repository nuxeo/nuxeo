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

import static org.nuxeo.elasticsearch.ElasticSearchConstants.DOC_TYPE;
import static org.nuxeo.elasticsearch.bulk.IndexAction.ACTION_FULL_NAME;
import static org.nuxeo.elasticsearch.bulk.IndexAction.ACTION_NAME;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.elasticsearch.Timestamp;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;

/**
 * Build elasticsearch requests to index documents.
 *
 * @since 10.3
 */
public class IndexRequestComputation extends AbstractBulkComputation {
    private static final Log log = LogFactory.getLog(IndexRequestComputation.class);

    // we want to avoid record bigger than 1MB because they requires specific configuration and impact performance
    protected static final long MAX_RECORD_SIZE = 900_000;

    protected static final String INDEX_OPTION = "indexName";

    protected BulkRequest bulkRequest;

    protected List<BulkRequest> bulkRequests = new ArrayList<>();

    protected String bucketKey;

    public IndexRequestComputation() {
        super(ACTION_FULL_NAME, 1);
    }

    @Override
    public void startBucket(String bucketKey) {
        this.bucketKey = bucketKey;
        bulkRequests.clear();
        bulkRequest = new BulkRequest();
    }

    @Override
    protected void compute(CoreSession session, List<String> documentIds, Map<String, Serializable> properties) {
        long now = Timestamp.currentTimeMicros();
        String indexName = getIndexName(session, properties);
        DocumentModelList docs = loadDocuments(session, documentIds);
        ElasticSearchIndexing esi = Framework.getService(ElasticSearchIndexing.class);
        for (DocumentModel doc : docs) {
            try {
                append(new IndexRequest(indexName, DOC_TYPE, doc.getId()).source(esi.source(doc), XContentType.JSON)
                                                                         .versionType(VersionType.EXTERNAL)
                                                                         .version(now));
            } catch (IOException e) {
                throw new NuxeoException("Cannot build source for document: " + doc.getId(), e);
            }
        }
    }

    protected void append(IndexRequest indexRequest) {
        if (bulkRequest.estimatedSizeInBytes() + indexRequest.source().length() > MAX_RECORD_SIZE) {
            if (bulkRequest.numberOfActions() > 0) {
                // Create multiple elastic bulk requests when we exceed the record size
                bulkRequests.add(bulkRequest);
                bulkRequest = new BulkRequest();
            }
            if (indexRequest.source().length() > MAX_RECORD_SIZE) {
                log.warn(String.format("Indexing request for doc: %s, is too large: %d, max record size: %d",
                        indexRequest.id(), indexRequest.source().length(), MAX_RECORD_SIZE));
            }
        }
        bulkRequest.add(indexRequest);
    }

    @Override
    public void endBucket(ComputationContext context, BulkStatus delta) {
        long bucketSize = delta.getProcessed();
        bulkRequests.add(bulkRequest);
        String commandId = getCurrentCommand().getId();
        int i = 0;
        int count = 0;
        for (BulkRequest request : bulkRequests) {
            DataBucket dataBucket = new DataBucket(commandId, request.numberOfActions(), toBytes(request));
            // use distinct key to distribute the message evenly between partitions
            String key = bucketKey + "-" + i++;
            context.produceRecord(OUTPUT_1, Record.of(key, BulkCodecs.getDataBucketCodec().encode(dataBucket)));
            count += request.numberOfActions();
        }
        if (count < bucketSize) {
            log.warn(String.format("Command: %s offset: %s created %d documents out of %d, %d not accessible",
                    commandId, context.getLastOffset(), count, bucketSize, bucketSize - count));
            DataBucket dataBucket = new DataBucket(commandId, bucketSize - count, toBytes(new BulkRequest()));
            context.produceRecord(OUTPUT_1,
                    Record.of(bucketKey + "-missing", BulkCodecs.getDataBucketCodec().encode(dataBucket)));
        }
        bulkRequest = null;
        bulkRequests.clear();
    }

    protected String getIndexName(CoreSession session, Map<String, Serializable> properties) {
        if (properties.containsKey(INDEX_OPTION)) {
            return (String) properties.get(INDEX_OPTION);
        }
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        return esa.getWriteIndexName(esa.getIndexNameForRepository(session.getRepositoryName()));
    }

    protected byte[] toBytes(BulkRequest request) {
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            request.writeTo(out);
            return BytesReference.toBytes(out.bytes());
        } catch (IOException e) {
            throw new NuxeoException("Cannot write elasticsearch bulk request " + request, e);
        }
    }

}

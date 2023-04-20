/*
 * (C) Copyright 2021 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.test.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.DataBucket;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.bulk.BulkIndexComputation;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.runtime.stream.StreamNoRetryException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.xcontent.XContentType;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class, CoreBulkFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestBulkIndexMalformedInput {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchIndexing esi;

    @Inject
    protected TransactionalFeature txFeature;

    protected DocumentModel addADocument() {
        String name = "a-file";
        DocumentModel doc = session.createDocumentModel("/", name, "File");
        doc.setPropertyValue("dc:title", "File title");
        doc = session.createDocument(doc);
        txFeature.nextTransaction();
        return doc;
    }

    @Test
    public void testMalformedInput() throws IOException {
        DocumentModel doc = addADocument();
        // Create a bulkIndex computation
        BulkIndexComputation comp = new BulkIndexComputation(10_000, 10, 100);
        ComputationContextImpl context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()));
        comp.init(context);

        // Corrupt the elastic doc representation replacing a boolean value with an integer
        String source = esi.source(doc).utf8ToString();
        source = source.replaceFirst("false", "123456789");
        Record record = createRecord(doc, source);

        // Submit the record to the computation
        comp.processRecord(context, "i1", record);
        // A status record is produced
        assertEquals(1, context.getRecords("o1").size());
        // Nothing is sent downstream, checkpoint is only done on processTimer
        assertFalse(context.requireCheckpoint());

        // Ask for the timer to flush the bulk processor and index
        assertThrows(StreamNoRetryException.class, () -> comp.processTimer(context, "flush", 0));
        // Nothing is sent downstream
        assertFalse(context.requireCheckpoint());
        comp.destroy();
    }

    @Test
    public void testValidInput() throws IOException {
        DocumentModel doc = addADocument();
        // Create a bulkIndex computation
        BulkIndexComputation comp = new BulkIndexComputation(10_000, 10, 100);
        ComputationContextImpl context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()));
        comp.init(context);

        // Submit the record to the computation
        String source = esi.source(doc).utf8ToString();
        comp.processRecord(context, "i1", createRecord(doc, source));
        assertEquals(1, context.getRecords("o1").size());
        assertFalse(context.requireCheckpoint());

        // Ask for the timer to flush the bulk processor and index the doc
        comp.processTimer(context, "flush", 0);
        // status record is sent downstream
        assertTrue(context.requireCheckpoint());
        assertFalse(context.requireTerminate());
        comp.destroy();
    }

    protected Record createRecord(DocumentModel doc, String source) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        IndexRequest req = new IndexRequest(esa.getIndexNameForRepository(doc.getRepositoryName())).id(
                doc.getId()).source(source, XContentType.JSON);
        bulkRequest.add(req);
        // Create a DataBucket record
        BytesStreamOutput out = new BytesStreamOutput();
        bulkRequest.writeTo(out);
        byte[] data = BytesReference.toBytes(out.bytes());
        DataBucket dataBucket = new DataBucket("commandId", 1, data);
        return Record.of("recordId", BulkCodecs.getDataBucketCodec().encode(dataBucket));
    }

}

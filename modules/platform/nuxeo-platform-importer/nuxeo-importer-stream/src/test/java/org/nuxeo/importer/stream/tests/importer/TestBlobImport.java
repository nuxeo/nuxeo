/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.importer.stream.tests.importer;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.importer.stream.StreamImporters.DEFAULT_LOG_BLOB_INFO_NAME;
import static org.nuxeo.importer.stream.StreamImporters.DEFAULT_LOG_BLOB_NAME;

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.importer.stream.StreamImporters;
import org.nuxeo.importer.stream.consumer.BlobInfoWriter;
import org.nuxeo.importer.stream.consumer.BlobMessageConsumerFactory;
import org.nuxeo.importer.stream.consumer.LogBlobInfoWriter;
import org.nuxeo.importer.stream.message.BlobInfoMessage;
import org.nuxeo.importer.stream.message.BlobMessage;
import org.nuxeo.importer.stream.producer.FileBlobMessageProducerFactory;
import org.nuxeo.importer.stream.producer.RandomStringBlobMessageProducerFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.pattern.consumer.BatchPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPool;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerStatus;
import org.nuxeo.lib.stream.pattern.producer.ProducerPool;
import org.nuxeo.lib.stream.pattern.producer.ProducerStatus;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.runtime.stream")
public abstract class TestBlobImport {
    protected static final Log log = LogFactory.getLog(TestBlobImport.class);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public abstract LogManager getManager() throws Exception;

    @Test
    public void randomStringBlob() throws Exception {
        final int NB_QUEUE = 10;
        final short NB_PRODUCERS = 10;
        final int NB_BLOBS = 2 * 1000;

        Codec<BlobMessage> blobCodec = StreamImporters.getBlobCodec();
        Codec<BlobInfoMessage> blobInfoCodec = StreamImporters.getBlobInfoCodec();
        try (LogManager manager = getManager()) {
            manager.createIfNotExists(Name.ofUrn(DEFAULT_LOG_BLOB_NAME), NB_QUEUE);

            ProducerPool<BlobMessage> producers = new ProducerPool<>(DEFAULT_LOG_BLOB_NAME, manager, blobCodec,
                    new RandomStringBlobMessageProducerFactory(NB_BLOBS, "en_US", 1, "1234"), NB_PRODUCERS);
            List<ProducerStatus> ret = producers.start().get();
            assertEquals(NB_PRODUCERS, ret.size());
            assertEquals(NB_PRODUCERS * NB_BLOBS, ret.stream().mapToLong(r -> r.nbProcessed).sum());

            try (LogManager managerBlobInfo = getManager()) {
                String blobProviderName = "test";
                managerBlobInfo.createIfNotExists(Name.ofUrn(DEFAULT_LOG_BLOB_INFO_NAME), 1);
                BlobInfoWriter blobInfoWriter = new LogBlobInfoWriter(
                        managerBlobInfo.getAppender(Name.ofUrn(DEFAULT_LOG_BLOB_INFO_NAME), blobInfoCodec));
                ConsumerPool<BlobMessage> consumers = new ConsumerPool<>(DEFAULT_LOG_BLOB_NAME, manager, blobCodec,
                        new BlobMessageConsumerFactory(blobProviderName, blobInfoWriter),
                        ConsumerPolicy.builder().batchPolicy(BatchPolicy.NO_BATCH).build());
                List<ConsumerStatus> retConsumers = consumers.start().get();
                assertEquals(NB_QUEUE, retConsumers.size());
                assertEquals(NB_PRODUCERS * NB_BLOBS, retConsumers.stream().mapToLong(r -> r.committed).sum());
            }
        }
    }

    @Test
    public void fileBlobImporter() throws Exception {
        final int NB_QUEUE = 2;
        final short NB_PRODUCERS = 2;
        final int NB_BLOBS = 50;

        Codec<BlobMessage> blobCodec = StreamImporters.getBlobCodec();
        Codec<BlobInfoMessage> blobInfoCodec = StreamImporters.getBlobInfoCodec();
        try (LogManager manager = getManager()) {
            manager.createIfNotExists(Name.ofUrn(DEFAULT_LOG_BLOB_NAME), NB_QUEUE);

            ProducerPool<BlobMessage> producers = new ProducerPool<>(DEFAULT_LOG_BLOB_NAME, manager, blobCodec,
                    new FileBlobMessageProducerFactory(getFileList("files/list.txt"), getBasePathList("files"),
                            NB_BLOBS),
                    NB_PRODUCERS);
            List<ProducerStatus> ret = producers.start().get();
            assertEquals(NB_PRODUCERS * NB_BLOBS, ret.stream().mapToLong(r -> r.nbProcessed).sum());

            try (LogManager managerBlobInfo = getManager()) {
                String blobProviderName = "test";
                managerBlobInfo.createIfNotExists(Name.ofUrn(DEFAULT_LOG_BLOB_INFO_NAME), 1);
                BlobInfoWriter blobInfoWriter = new LogBlobInfoWriter(
                        managerBlobInfo.getAppender(Name.ofUrn(DEFAULT_LOG_BLOB_INFO_NAME), blobInfoCodec));
                ConsumerPool<BlobMessage> consumers = new ConsumerPool<>(DEFAULT_LOG_BLOB_NAME, manager, blobCodec,
                        new BlobMessageConsumerFactory(blobProviderName, blobInfoWriter),
                        ConsumerPolicy.builder().batchPolicy(BatchPolicy.NO_BATCH).build());
                List<ConsumerStatus> retConsumers = consumers.start().get();
                assertEquals(NB_PRODUCERS * NB_BLOBS, retConsumers.stream().mapToLong(r -> r.committed).sum());
            }
        }
    }

    protected File getFileList(String filename) {
        return new File(this.getClass().getClassLoader().getResource(filename).getPath());
    }

    protected String getBasePathList(String base) {
        return this.getClass().getClassLoader().getResource(base).getPath();
    }

}

/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.importer.stream.automation;

import static org.nuxeo.importer.stream.StreamImporters.DEFAULT_LOG_BLOB_INFO_NAME;
import static org.nuxeo.importer.stream.StreamImporters.DEFAULT_LOG_BLOB_NAME;
import static org.nuxeo.importer.stream.StreamImporters.DEFAULT_LOG_CONFIG;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.importer.stream.StreamImporters;
import org.nuxeo.importer.stream.consumer.BlobInfoWriter;
import org.nuxeo.importer.stream.consumer.BlobMessageConsumerFactory;
import org.nuxeo.importer.stream.consumer.LogBlobInfoWriter;
import org.nuxeo.importer.stream.message.BlobInfoMessage;
import org.nuxeo.importer.stream.message.BlobMessage;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.pattern.consumer.BatchPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPool;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

import net.jodah.failsafe.RetryPolicy;

/**
 * @since 9.1
 */
@Operation(id = BlobConsumers.ID, category = Constants.CAT_SERVICES, label = "Import blobs", since = "9.1", description = "Import blob into the binarystore.")
public class BlobConsumers {
    private static final Log log = LogFactory.getLog(BlobConsumers.class);

    public static final String ID = "StreamImporter.runBlobConsumers";

    @Context
    protected OperationContext ctx;

    @Param(name = "nbThreads", required = false)
    protected Integer nbThreads;

    @Param(name = "blobProviderName", required = false)
    protected String blobProviderName = "default";

    @Param(name = "batchSize", required = false)
    protected Integer batchSize = 10;

    @Param(name = "batchThresholdS", required = false)
    protected Integer batchThresholdS = 20;

    @Param(name = "retryMax", required = false)
    protected Integer retryMax = 3;

    @Param(name = "retryDelayS", required = false)
    protected Integer retryDelayS = 2;

    @Param(name = "logName", required = false)
    protected String logName = DEFAULT_LOG_BLOB_NAME;

    @Param(name = "logBlobInfo", required = false)
    protected String logBlobInfoName = DEFAULT_LOG_BLOB_INFO_NAME;

    @Param(name = "logConfig", required = false)
    protected String logConfig = DEFAULT_LOG_CONFIG;

    @Param(name = "waitMessageTimeoutSeconds", required = false)
    protected Integer waitMessageTimeoutSeconds = 20;

    @Param(name = "watermark", required = false)
    protected String watermark;

    @Param(name = "persistBlobPath", required = false)
    protected String persistBlobPath;

    @OperationMethod
    public void run() throws OperationException {
        RandomBlobProducers.checkAccess(ctx);
        ConsumerPolicy consumerPolicy = ConsumerPolicy.builder()
                                                      .name(ID)
                                                      // we set the batch policy but batch is not used by the blob
                                                      // consumer
                                                      .batchPolicy(
                                                              BatchPolicy.builder()
                                                                         .capacity(batchSize)
                                                                         .timeThreshold(
                                                                                 Duration.ofSeconds(batchThresholdS))
                                                                         .build())
                                                      .retryPolicy(new RetryPolicy().withMaxRetries(retryMax)
                                                                                    .withDelay(retryDelayS,
                                                                                            TimeUnit.SECONDS))
                                                      .maxThreads(getNbThreads())
                                                      .waitMessageTimeout(Duration.ofSeconds(waitMessageTimeoutSeconds))
                                                      .build();
        LogManager manager = Framework.getService(StreamService.class).getLogManager(logConfig);
        Codec<BlobMessage> codec = StreamImporters.getBlobCodec();
        try (BlobInfoWriter blobInfoWriter = getBlobInfoWriter(manager)) {
            ConsumerPool<BlobMessage> consumers = new ConsumerPool<>(logName, manager, codec,
                    new BlobMessageConsumerFactory(blobProviderName, blobInfoWriter, watermark, persistBlobPath),
                    consumerPolicy);
            consumers.start().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Operation interrupted");
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.error("Operation fails", e);
            throw new OperationException(e);
        }
    }

    protected BlobInfoWriter getBlobInfoWriter(LogManager managerBlobInfo) {
        initBlobInfoMQ(managerBlobInfo);
        Codec<BlobInfoMessage> blobInfoCodec = StreamImporters.getBlobInfoCodec();
        return new LogBlobInfoWriter(managerBlobInfo.getAppender(Name.ofUrn(logBlobInfoName), blobInfoCodec));
    }

    protected void initBlobInfoMQ(LogManager manager) {
        manager.createIfNotExists(Name.ofUrn(logBlobInfoName), 1);
    }

    protected short getNbThreads() {
        if (nbThreads != null) {
            return nbThreads.shortValue();
        }
        return 0;
    }
}

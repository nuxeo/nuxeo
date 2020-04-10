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

import static org.nuxeo.importer.stream.StreamImporters.DEFAULT_LOG_CONFIG;
import static org.nuxeo.importer.stream.StreamImporters.DEFAULT_LOG_DOC_NAME;

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
import org.nuxeo.importer.stream.consumer.DocumentConsumerPolicy;
import org.nuxeo.importer.stream.consumer.DocumentConsumerPool;
import org.nuxeo.importer.stream.consumer.DocumentMessageConsumerFactory;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.pattern.consumer.BatchPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

import net.jodah.failsafe.RetryPolicy;

/**
 * @since 9.1
 */
@Operation(id = DocumentConsumers.ID, category = Constants.CAT_SERVICES, label = "Imports document", since = "9.1", description = "Import documents into repository.")
public class DocumentConsumers {
    private static final Log log = LogFactory.getLog(DocumentConsumers.class);

    public static final String ID = "StreamImporter.runDocumentConsumers";

    @Context
    protected OperationContext ctx;

    @Param(name = "nbThreads", required = false)
    protected Integer nbThreads;

    @Param(name = "rootFolder")
    protected String rootFolder;

    @Param(name = "repositoryName", required = false)
    protected String repositoryName;

    @Param(name = "batchSize", required = false)
    protected Integer batchSize = 10;

    @Param(name = "batchThresholdS", required = false)
    protected Integer batchThresholdS = 20;

    @Param(name = "retryMax", required = false)
    protected Integer retryMax = 3;

    @Param(name = "retryDelayS", required = false)
    protected Integer retryDelayS = 2;

    @Param(name = "logName", required = false)
    protected String logName = DEFAULT_LOG_DOC_NAME;

    @Param(name = "blockIndexing", required = false)
    protected Boolean blockIndexing = false;

    @Param(name = "blockAsyncListeners", required = false)
    protected Boolean blockAsyncListeners = false;

    @Param(name = "blockPostCommitListeners", required = false)
    protected Boolean blockPostCommitListeners = false;

    @Param(name = "blockDefaultSyncListeners", required = false)
    protected Boolean blockSyncListeners = false;

    @Param(name = "useBulkMode", required = false)
    protected Boolean useBulkMode = false;

    @Param(name = "waitMessageTimeoutSeconds", required = false)
    protected Integer waitMessageTimeoutSeconds = 20;

    @OperationMethod
    public void run() throws OperationException {
        RandomBlobProducers.checkAccess(ctx);
        repositoryName = getRepositoryName();
        ConsumerPolicy consumerPolicy = DocumentConsumerPolicy.builder()
                                                              .blockIndexing(blockIndexing)
                                                              .blockAsyncListeners(blockAsyncListeners)
                                                              .blockPostCommitListeners(blockPostCommitListeners)
                                                              .blockDefaultSyncListener(blockSyncListeners)
                                                              .useBulkMode(useBulkMode)
                                                              .name(ID)
                                                              .batchPolicy(BatchPolicy.builder()
                                                                                      .capacity(batchSize)
                                                                                      .timeThreshold(Duration.ofSeconds(
                                                                                              batchThresholdS))
                                                                                      .build())
                                                              .retryPolicy(new RetryPolicy().withMaxRetries(retryMax)
                                                                                            .withDelay(retryDelayS,
                                                                                                    TimeUnit.SECONDS))
                                                              .maxThreads(getNbThreads())
                                                              .waitMessageTimeout(
                                                                      Duration.ofSeconds(waitMessageTimeoutSeconds))
                                                              .salted()
                                                              .build();
        log.warn(String.format("Import documents from log: %s into: %s/%s, with policy: %s", logName, repositoryName,
                rootFolder, consumerPolicy));
        LogManager manager = Framework.getService(StreamService.class).getLogManager();
        Codec<DocumentMessage> codec = StreamImporters.getDocCodec();
        try (DocumentConsumerPool<DocumentMessage> consumers = new DocumentConsumerPool<>(logName, manager, codec,
                new DocumentMessageConsumerFactory(repositoryName, rootFolder), consumerPolicy)) {
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

    protected short getNbThreads() {
        if (nbThreads != null) {
            return nbThreads.shortValue();
        }
        return 0;
    }

    protected String getRepositoryName() {
        if (repositoryName != null && !repositoryName.isEmpty()) {
            return repositoryName;
        }
        return ctx.getCoreSession().getRepositoryName();
    }
}

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
import org.nuxeo.importer.stream.consumer.RedisDocumentMessageConsumerFactory;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.pattern.consumer.BatchPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPool;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

import net.jodah.failsafe.RetryPolicy;

/**
 * Import document message into Redis, so they can be used by Gatling simulation to create Nuxeo documents.
 *
 * @since 10.2
 */
@Operation(id = RedisDocumentConsumers.ID, category = Constants.CAT_SERVICES, label = "Imports document into Redis", since = "10.1", description = "Import documents into Redis.")
public class RedisDocumentConsumers {
    private static final Log log = LogFactory.getLog(RedisDocumentConsumers.class);

    public static final String ID = "StreamImporter.runRedisDocumentConsumers";

    @Context
    protected OperationContext ctx;

    @Param(name = "nbThreads", required = false)
    protected Integer nbThreads;

    @Param(name = "redisPrefix", required = false)
    protected String redisPrefix;

    @Param(name = "retryMax", required = false)
    protected Integer retryMax = 3;

    @Param(name = "retryDelayS", required = false)
    protected Integer retryDelayS = 2;

    @Param(name = "logName", required = false)
    protected String logName = DEFAULT_LOG_DOC_NAME;

    @Param(name = "waitMessageTimeoutSeconds", required = false)
    protected Integer waitMessageTimeoutSeconds = 20;

    @OperationMethod
    public void run() throws OperationException {
        RandomBlobProducers.checkAccess(ctx);
        ConsumerPolicy consumerPolicy = ConsumerPolicy.builder()
                                                      .name(ID)
                                                      .batchPolicy(BatchPolicy.NO_BATCH)
                                                      .retryPolicy(new RetryPolicy().withMaxRetries(retryMax).withDelay(
                                                              retryDelayS, TimeUnit.SECONDS))
                                                      .maxThreads(getNbThreads())
                                                      .waitMessageTimeout(Duration.ofSeconds(waitMessageTimeoutSeconds))
                                                      .build();
        log.warn(String.format("Import documents into Redis from log: %s, with policy: %s", logName, consumerPolicy));
        LogManager manager = Framework.getService(StreamService.class).getLogManager();
        Codec<DocumentMessage> codec = StreamImporters.getDocCodec();
        try (ConsumerPool<DocumentMessage> consumers = new ConsumerPool<>(logName, manager, codec,
                new RedisDocumentMessageConsumerFactory(redisPrefix), consumerPolicy)) {
            consumers.start().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Operation interrupted");
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.error("fail", e);
            throw new OperationException(e);
        }
    }

    protected short getNbThreads() {
        if (nbThreads != null) {
            return nbThreads.shortValue();
        }
        return 0;
    }
}

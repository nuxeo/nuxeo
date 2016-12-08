/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.importer.executor.jaxrs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunner;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.QueueImporter;
import org.nuxeo.ecm.platform.importer.queue.consumer.ConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.consumer.ConsumerFactoryImpl;
import org.nuxeo.ecm.platform.importer.queue.manager.BQManager;
import org.nuxeo.ecm.platform.importer.queue.manager.CQManager;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.ecm.platform.importer.queue.producer.Producer;
import org.nuxeo.ecm.platform.importer.queue.producer.SourceNodeProducer;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("randomQImporter")
public class RandomQImporterExecutor extends AbstractJaxRSImporterExecutor {

    private static final Log log = LogFactory.getLog(RandomQImporterExecutor.class);

    @Override
    protected Log getJavaLogger() {
        return log;
    }

    @GET
    @Path("run")
    @Produces("text/plain; charset=UTF-8")
    public String run(@QueryParam("targetPath") String targetPath,
            @QueryParam("batchSize") Integer batchSize, @QueryParam("nbThreads") Integer nbThreads,
            @QueryParam("nbNodes") Integer nbNodes,
            @QueryParam("fileSizeKB") Integer fileSizeKB, @QueryParam("onlyText") Boolean onlyText,
            @QueryParam("nonUniform") Boolean nonUniform, @QueryParam("withProperties") Boolean withProperties,
            @QueryParam("transactionTimeout") Integer transactionTimeout,
            @QueryParam("queueType") String queueType,
            @QueryParam("lang") String lang) {
        if (onlyText == null) {
            onlyText = true;
        }
        if (nonUniform == null) {
            nonUniform = false;
        }
        if (withProperties == null) {
            withProperties = false;
        }
        if (queueType == null) {
            queueType = "CQ";
        }
        log.info("Init Random text generator");
        SourceNode source = RandomTextSourceNode.init(nbNodes, fileSizeKB, onlyText, nonUniform, withProperties, lang);
        log.info("Random text generator initialized");

        QueueImporter importer = new QueueImporter(getLogger());
        QueuesManager qm;
        switch (queueType) {
            case "BQ":
                log.info("Using in memory BlockingQueue");
                qm = new BQManager(getLogger(), nbThreads, 10000);
                break;
            case "CQ":
            default:
                log.info("Using Off heap Chronicle Queue");
                qm = new CQManager(getLogger(), nbThreads);
        }
        Producer producer = new SourceNodeProducer(source, getLogger());
        ConsumerFactory consumerFactory = new ConsumerFactoryImpl();
        if (transactionTimeout != null) {
            Framework.getService(DefaultImporterService.class).setTransactionTimeout(transactionTimeout);
        }
        log.warn(String.format("Running import of %d documents into %s with %d consumers, commit batch size: %d", nbNodes, targetPath, nbThreads, batchSize ));
        importer.importDocuments(producer, qm, targetPath, "default", batchSize, consumerFactory);
        log.warn("Import terminated, created:" + importer.getCreatedDocsCounter());
        return "OK " + importer.getCreatedDocsCounter();
    }

    @Override
    public String run(ImporterRunner runner, Boolean interactive) {
        return doRun(runner, interactive);
    }

}

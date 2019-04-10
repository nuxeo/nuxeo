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
 */
package org.nuxeo.ecm.platform.importer.queue;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.consumer.Consumer;
import org.nuxeo.ecm.platform.importer.queue.consumer.ConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.consumer.ImportStat;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.ecm.platform.importer.queue.producer.Producer;

/**
 * @since 8.3
 */
public class QueueImporter {

    protected ImporterLogger log = null;

    private boolean isTerminated = false;

    private boolean mustStop = false;

    protected long processedNodesConsumer = 0L;

    protected ImportStat importStat = new ImportStat();

    protected long nbDocsCreated = 0L;

    protected List<ImporterFilter> filters = new ArrayList<ImporterFilter>();

    public QueueImporter(ImporterLogger log) {
        this.log = log;
    }

    public void mustStop() {
        mustStop = true;
    }

    public void importDocuments(Producer producer, QueuesManager manager, String importPath, String repositoryName,
            int batchSize, ConsumerFactory factory) {
        log.info("Starting import process");

        producer.init(manager);

        // start the producer
        Thread p = new Thread(producer);
        p.setName("import-Producer");

        p.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                log.error("Uncaught exception in " + p.getName() + ". Producer is going to be stopped", e);
                producer.mustStop();
            }
        });
        p.start();

        Exception finalException = null;
        CoreSession session = CoreInstance.openCoreSessionSystem(repositoryName);
        DateTime importStarted = new DateTime();
        try {
            List<Thread> consumerThreads = new ArrayList<Thread>();
            List<Consumer> consumers = new ArrayList<Consumer>();
            enableFilters();

            DocumentModel root = session.getDocument(new PathRef(importPath));
            for (int i = 0; i < manager.getNBConsumers(); i++) {
                Consumer c;
                c = factory.createConsumer(log, root, batchSize, manager.getQueue(i));

                consumers.add(c);
                Thread ct = new Thread(c);

                ct.setName("import-Consumer" + i);
                ct.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.error("Uncaught exception in " + ct.getName() + ". Consumer is going to be stopped", e);
                        c.mustStop();
                    }
                });
                ct.start();
                consumerThreads.add(ct);
            }

            try {
                while (!producer.isTerminated() && !mustStop) {
                    Thread.sleep(50);
                    log.debug("waiting for producer to be completed. Processed docs: " + producer.getNbProcessed());

                    // Check if consumers are still alive
                    boolean consumersTerminated = true;
                    for (Consumer c : consumers) {
                        consumersTerminated = consumersTerminated && c.isTerminated();
                    }
                    // Otherwise stop the producer
                    if (consumersTerminated) {
                        log.error("all consumers are terminated,but producer is still alive. Stopping producer.");
                        producer.mustStop();
                    }
                }
            } catch (InterruptedException e) {
                log.error("Error while waiting for producder", e);
                finalException = e;
                ExceptionUtils.checkInterrupt(e);
            } finally {
                if (!producer.isTerminated()) {
                    log.warn("Forcibly stopping producer");
                    producer.mustStop();
                }
            }

            Exception pe = producer.getError();
            if (pe != null) {
                log.error("Error during producer execution", pe);
                finalException = pe;
                for (Consumer c : consumers) {
                    c.mustStop();
                }
            } else {
                for (Consumer c : consumers) {
                    c.canStop();
                }
            }

            try {
                int iLoop = 0;

                while (!isTerminated && !mustStop) {
                    double totalImmediateThroughput = 0;
                    double totalThroughput = 0;
                    nbDocsCreated = 0;

                    Thread.sleep(100);
                    isTerminated = true;
                    iLoop++;
                    for (Consumer c : consumers) {
                        isTerminated = isTerminated && c.isTerminated();
                        nbDocsCreated += c.getNbDocsCreated();
                        totalImmediateThroughput += c.getImmediateThroughput();
                        totalThroughput += c.getThroughput();
                    }

                    if (iLoop % 30 == 0) {
                        log.debug("waiting for consumers to be completed. Processed Docs: " + nbDocsCreated + " -- "
                                + totalImmediateThroughput + " docs/s -- " + totalThroughput + " docs/s");
                    }
                }
                // Consumers are done, get total number of nodes imported
                for (Consumer c : consumers) {
                    processedNodesConsumer += c.getNbProcessed();
                }
                nbDocsCreated = 0;
                // Consumers are done, get total number of docs created
                for (Consumer c : consumers) {
                    nbDocsCreated += c.getNbDocsCreated();
                    importStat.merge(c.getImportStat());
                }

            } catch (InterruptedException e) {
                log.error("Error while waiting for consumers", e);
                finalException = e;
                ExceptionUtils.checkInterrupt(e);
            } finally {
                for (Consumer c : consumers) {
                    if (!c.isTerminated()) {
                        log.warn("Forcibly stopping consumer");
                        c.mustStop();
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error while importing", e);
            finalException = e;
        } finally {
            disableFilters(finalException);
            if (session != null) {
                session.close();
                session = null;
            }
        }

        if (producer.getNbProcessed() != processedNodesConsumer) {
            log.error(
                    String.format("Producer produced %s nodes, Consumers processed %s nodes, some nodes have been lost",
                            producer.getNbProcessed(), processedNodesConsumer));
        }

        DateTime importFinished = new DateTime();

        log.info(String.format("End of import process : Imported %s docs in %s mn. ", nbDocsCreated,
                Minutes.minutesBetween(importStarted, importFinished).getMinutes()));

    }

    public ImportStat getImportStat() {
        return importStat;
    }

    public void addFilter(ImporterFilter filter) {
        log.debug(String.format("Filter with %s, was added on the importer with the hash code %s.", filter.toString(),
                hashCode()));
        filters.add(filter);
    }

    public boolean isRunning() {
        return !isTerminated;
    }

    public long getCreatedDocsCounter() {
        return nbDocsCreated;
    }

    protected void enableFilters() {
        for (ImporterFilter filter : filters) {
            log.debug(String.format("Running filter with %s, on the importer with the hash code %s.", filter.toString(),
                    hashCode()));
            filter.handleBeforeImport();
        }
        if (filters.size() == 0) {
            log.debug(String.format("No filters are registered on the importer with hash code %s", hashCode()));
        }
    }

    protected void disableFilters(Exception finalException) {
        for (ImporterFilter filter : filters) {
            filter.handleAfterImport(finalException);
        }

    }

}

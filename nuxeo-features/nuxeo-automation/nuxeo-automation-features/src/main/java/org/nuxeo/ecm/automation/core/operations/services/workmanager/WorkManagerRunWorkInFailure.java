/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.operations.services.workmanager;

import static org.nuxeo.ecm.core.work.WorkManagerImpl.DEAD_LETTER_QUEUE;
import static org.nuxeo.ecm.core.work.WorkManagerImpl.DEFAULT_LOG_MANAGER;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.work.WorkComputation;
import org.nuxeo.ecm.core.work.WorkHolder;
import org.nuxeo.ecm.core.work.WorkManagerImpl;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

import net.jodah.failsafe.RetryPolicy;

/**
 * Executes Works stored in the dead letter queue after failure.
 *
 * @since 11.1
 */
@Operation(id = WorkManagerRunWorkInFailure.ID, category = Constants.CAT_SERVICES, label = "Executes Works stored in the dead letter queue", addToStudio = false, description = "Try to execute again Works that have been send to a dead letter queue by the WorkManager after failure")
public class WorkManagerRunWorkInFailure {
    private static final Logger log = LogManager.getLogger(WorkManagerRunWorkInFailure.class);

    public static final String ID = "WorkManager.RunWorkInFailure";

    protected static final long DEFAULT_TIMEOUT_SECONDS = 3600L;

    protected static final long ASSIGNMENT_TIMEOUT_SECONDS = 60L;

    protected volatile long countTotal;

    protected volatile long countSuccess;

    protected volatile long countFiltered;

    @Param(name = "timeoutSeconds", required = false)
    protected long timeout = DEFAULT_TIMEOUT_SECONDS;

    // @since 11.5
    @Param(name = "dryRun", required = false)
    protected boolean dryRun = false;

    // @since 11.5
    @Param(name = "categoryFilter", required = false)
    protected StringList categoryFilter;

    @OperationMethod
    public Blob run() throws IOException, InterruptedException, TimeoutException {
        log.warn("Reprocessing Works in DLQ: dryRun: {}, categoryFilter: {}, timeout after: {}s", dryRun, categoryFilter, timeout);
        StreamManager streamManager = Framework.getService(StreamService.class).getStreamManager(DEFAULT_LOG_MANAGER);
        Settings settings = new Settings(1, 1, WorkManagerImpl.DEAD_LETTER_QUEUE_CODEC, getComputationPolicy());
        StreamProcessor processor = streamManager.registerAndCreateProcessor("RunWorkInFailure", getTopology(),
                settings);
        boolean timeoutDuringProcessing;
        try {
            countTotal = 0;
            countSuccess = 0;
            countFiltered = 0;
            processor.start();
            processor.waitForAssignments(Duration.ofSeconds(ASSIGNMENT_TIMEOUT_SECONDS));
            timeoutDuringProcessing = !processor.drainAndStop(getTimeout());
        } finally {
            processor.shutdown();
        }
        Blob result = buildResult();
        log.warn("Reprocessing status: {}, timeout: {}", result.getString(), timeoutDuringProcessing);
        if (timeoutDuringProcessing) {
            throw new TimeoutException();
        }
        return result;
    }

    protected Blob buildResult() throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("total", countTotal);
        result.put("success", countSuccess);
        result.put("filtered", countFiltered);
        return Blobs.createJSONBlobFromValue(result);
    }

    protected Duration getTimeout() {
        return Duration.ofSeconds(timeout);
    }

    protected ComputationPolicy getComputationPolicy() {
        return new ComputationPolicyBuilder().retryPolicy(new RetryPolicy(ComputationPolicy.NO_RETRY))
                                             .continueOnFailure(true)
                                             .build();
    }

    protected Topology getTopology() {
        return Topology.builder()
                       .addComputation(WorkFailureComputation::new,
                               Collections.singletonList(INPUT_1 + ":" + DEAD_LETTER_QUEUE))
                       .build();
    }

    protected class WorkFailureComputation extends AbstractComputation {

        private static final String NAME = "WorkFailure";

        public WorkFailureComputation() {
            super(NAME, 1, 0);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            if (dryRun) {
                context.cancelAskForCheckpoint();
            } else {
                context.askForCheckpoint();
            }
            Work work = WorkComputation.deserialize(record.getData());
            if (toBeSkipped(work)) {
                return;
            }
            log.info("Trying to reprocess work: {} with category: {}", work::getId, work::getCategory);
            try {
                if (!dryRun) {
                    // Using a non RUNNING state to prevent the Work to go in the DLQ
                    work.setWorkInstanceState(Work.State.UNKNOWN);
                    new WorkHolder(work).run();
                    cleanup(work, null);
                    log.info("Work: {} successfully reprocessed", work::getId);
                }
                countSuccess++;
            } catch (Exception e) {
                cleanup(work, e);
                log.error("Fail to reprocess work: {} with category: {}", work.getId(), work.getCategory(), e);
            }
            countTotal++;
        }

        protected boolean toBeSkipped(Work work) {
            if (categoryFilter == null || categoryFilter.isEmpty() || categoryFilter.contains(work.getCategory())) {
                log.debug("Keep work: {} with category: {}", work::getId, work::getCategory);
                return false;
            }
            log.info("Skipping work: {} with category: {}", work::getId, work::getCategory);
            countFiltered++;
            return true;
        }

        protected void cleanup(Work work, Exception exception) {
            try {
                work.cleanUp(true, exception);
            } catch (Exception e) {
                log.error(work.getId() + ": Failure on cleanup", e);
            }
        }
    }
}

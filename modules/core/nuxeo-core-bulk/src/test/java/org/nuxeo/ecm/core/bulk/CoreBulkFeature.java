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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.BULK_KV_STORE_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_PREFIX;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterFeature;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Intermediate feature for nuxeo-core-bulk module.
 *
 * @since 10.2
 */
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.ecm.core.bulk")
@Deploy("org.nuxeo.ecm.core.bulk.test")
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.core.io")
@Features({ ClusterFeature.class, TransactionalFeature.class, RuntimeStreamFeature.class })
public class CoreBulkFeature implements RunnerFeature {

    protected List<BulkActionTriggeredByEventWaiter> bulkActionTriggeredByEventWaiters = new ArrayList<>();

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class)
              .addWaiter(duration -> Framework.getService(BulkService.class).await(duration));
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) {
        bulkActionTriggeredByEventWaiters.forEach(BulkActionTriggeredByEventWaiter::startListening);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        bulkActionTriggeredByEventWaiters.forEach(BulkActionTriggeredByEventWaiter::stopListening);
    }

    /**
     * Add a waiter for bulk command submitted by event listener.
     *
     * @param runner the features runner
     * @param action wait for the bulk commands that belong to this action
     * @param event name of the event that triggers the listener submitting bulk commands
     * @since 2025.9
     */
    public void addBulkCommandWaiterForListener(FeaturesRunner runner, String action, String event) {
        var waiter = new BulkActionTriggeredByEventWaiter(action, event);
        runner.getFeature(TransactionalFeature.class).addWaiter(waiter);
        bulkActionTriggeredByEventWaiters.add(waiter);
    }

    public boolean wait(String action, Duration duration) throws InterruptedException {
        var leftDuration = duration;
        long begin = System.currentTimeMillis();
        var kv = (KeyValueStoreProvider) Framework.getService(KeyValueService.class)
                                                  .getKeyValueStore(BULK_KV_STORE_NAME);
        var commandIds = kv.keyStream(STATUS_PREFIX)
                           .map(kv::get)
                           .map(BulkCodecs.getStatusCodec()::decode)
                           .filter(status -> action.equals(status.getAction()))
                           .map(BulkStatus::getId)
                           .collect(Collectors.toSet());
        var bulkService = Framework.getService(BulkService.class);
        for (String commandId : commandIds) {
            if (!bulkService.await(commandId, leftDuration)) {
                return false;
            }
            leftDuration = duration.minusMillis(System.currentTimeMillis() - begin);
            begin = System.currentTimeMillis();
            if (leftDuration.isNegative()) {
                // bulk service consumed all the permitted duration
                return false;
            }
        }
        return true;
    }

    protected class BulkActionTriggeredByEventWaiter implements TransactionalFeature.Waiter {

        protected final String actionName;

        protected final String eventName;

        protected CapturingEventListener capturingEventListener = null;

        public BulkActionTriggeredByEventWaiter(String actionName, String eventName) {
            this.actionName = actionName;
            this.eventName = eventName;
        }

        @Override
        public boolean await(Duration duration) throws InterruptedException {
            if (capturingEventListener != null && !capturingEventListener.getCapturedEvents().isEmpty()) {
                // first wait for event to be processed
                long begin = System.currentTimeMillis();
                if (Framework.getService(WorkManager.class)
                             .awaitCompletion(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                    var leftDuration = duration.minusMillis(System.currentTimeMillis() - begin);
                    // second wait for Bulk Action
                    return CoreBulkFeature.this.wait(actionName, leftDuration);
                }
                // work manager consumed all the permitted duration
                return false;
            }
            return true;
        }

        protected void startListening() {
            capturingEventListener = new CapturingEventListener(eventName);
        }

        protected void stopListening() {
            if (capturingEventListener != null) {
                capturingEventListener.close();
                capturingEventListener = null;
            }
        }

    }

}

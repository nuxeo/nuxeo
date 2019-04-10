/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.quota.count;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author dmetzler
 */
public class IsolatedSessionRunner {

    private CoreSession session;

    private EventService eventService;

    private int waitTimeInMs = 300;

    /**
     * @param session
     * @throws Exception
     */
    public IsolatedSessionRunner(CoreSession session, EventService eventService) throws Exception {
        this.session = session;
        this.eventService = eventService;
    }

    /**
     * @param runnable
     * @throws Exception
     */
    public void run(RunnableWithException runnable) throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        try {
            runnable.run();
            session.save();
        } finally {

            TransactionHelper.commitOrRollbackTransaction();
            Thread.sleep(getWaitTimeInMs());
            eventService.waitForAsyncCompletion();
            TransactionHelper.startTransaction();
        }

    }

    public long getWaitTimeInMs() {
        return waitTimeInMs;
    }

    public void setWaitTimeInMs(int waitTimeInMs) {
        this.waitTimeInMs = waitTimeInMs;
    }

}

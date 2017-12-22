/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.event.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Abstract class that helps building an Asynchronous listeners that will handle a long running process.
 * <p/>
 * By default, {@link org.nuxeo.ecm.core.event.PostCommitEventListener} are executed in a
 * {@link org.nuxeo.ecm.core.work.api.Work} that will take care of starting/comitting the transaction.
 * <p/>
 * If the listener requires a long processing this will create long transactions that are not good. To avoid this
 * behavior, this base class split the processing in 3 steps :
 * <ul>
 * <li>pre processing : transactional first step</li>
 * <li>long running : long running processing that should not require transactional resources</li>
 * <li>post processing : transactional final step
 * </ul>
 * <p/>
 * To manage sharing between the 3 steps, a simple Map is provided.
 *
 * @since 5.7.2
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public abstract class AbstractLongRunningListener implements PostCommitFilteringEventListener {

    protected static final Log log = LogFactory.getLog(AbstractLongRunningListener.class);

    @Override
    public void handleEvent(EventBundle events) {

        Map<String, Object> data = new HashMap<String, Object>();

        if (events instanceof ReconnectedEventBundleImpl) {

            boolean doContinue = false;

            // do pre-processing in a transaction
            // a new CoreSession will be open by ReconnectedEventBundleImpl
            ReconnectedEventBundleImpl preProcessBunle = new ReconnectedEventBundleImpl(events);
            try {
                doContinue = handleEventPreprocessing(preProcessBunle, data);
            } finally {
                preProcessBunle.disconnect();
            }

            if (!doContinue) {
                return;
            }

            // do main-processing in a non transactional context
            TransactionHelper.commitOrRollbackTransaction();
            try {
                doContinue = handleEventLongRunning(((ReconnectedEventBundleImpl) events).getEventNames(), data);
            } finally {
                TransactionHelper.startTransaction();
            }

            if (!doContinue) {
                return;
            }

            // do final-processing in a new transaction
            // a new CoreSession will be open by ReconnectedEventBundleImpl
            ReconnectedEventBundleImpl postProcessEventBundle = new ReconnectedEventBundleImpl(events);
            try {
                handleEventPostprocessing(postProcessEventBundle, data);
            } finally {
                postProcessEventBundle.disconnect();
            }
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        } else {
            log.error("Unable to execute long running listener, input EventBundle is not a ReconnectedEventBundle");
        }
    }

    /**
     * Handles first step of processing in a normal transactional way.
     *
     * @param events {@link EventBundle} received
     * @param data an empty map to store data to share data between steps.
     * @return true of processing should continue, false otherwise
     */
    protected abstract boolean handleEventPreprocessing(EventBundle events, Map<String, Object> data);

    /**
     * Will be executed in a non transactional context
     * <p/>
     * Any acess to a CoreSession will generate WARN in the the logs.
     * <p/>
     * Documents passed with data should not be connected.
     *
     * @param eventNames list of event names
     * @param data an map that may have been filled by handleEventPreprocessing
     * @return true of processing should continue, false otherwise
     */
    protected abstract boolean handleEventLongRunning(List<String> eventNames, Map<String, Object> data);

    /**
     * Finish processing in a dedicated Transaction
     *
     * @param events {@link EventBundle} received
     * @param data an map that may have been filled by handleEventPreprocessing and handleEventLongRunning
     */
    protected abstract void handleEventPostprocessing(EventBundle events, Map<String, Object> data);

}

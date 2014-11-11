/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Abstract class that helps building an Asynchronous listeners that will handle
 * a long running process.
 * <p/>
 * By default, {@link org.nuxeo.ecm.core.event.PostCommitEventListener} are executed in a {@link org.nuxeo.ecm.core.work.api.Work}
 * that will take care of starting/comitting the transaction.
 * <p/>
 * If the listener requires a long processing this will create long transactions
 * that are not good. To avoid this behavior, this base class split the
 * processing in 3 steps :
 * <ul>
 * <li>pre processing : transactional first step</li>
 * <li>long running : long running processing that should not require
 * transactional resources</li>
 * <li>post processing : transactional final step
 * </ul>
 * <p/>
 * To manage sharing between the 3 steps, a simple Map is provided.
 *
 * @since 5.7.2
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public abstract class AbstractLongRunningListener implements
        PostCommitFilteringEventListener {

    protected static final Log log = LogFactory.getLog(AbstractLongRunningListener.class);

    @Override
    public void handleEvent(EventBundle events) throws ClientException {

        Map<String, Object> data = new HashMap<String, Object>();

        if (events instanceof ReconnectedEventBundleImpl) {

            boolean doContinue = false;
            // do pre-processing and commit transaction
            ReconnectedEventBundleImpl preProcessBunle = new ReconnectedEventBundleImpl(
                    events);
            try {
                doContinue = handleEventPreprocessing(preProcessBunle, data);
            } catch (ClientException e) {
                log.error(
                        "Long Running listener canceled after failed execution of preprocessing",
                        e);
                throw e;
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                preProcessBunle.disconnect();
            }
            if (!doContinue) {
                return;
            }

            // do main-processing in a non transactional context
            // a new CoreSession will be open by ReconnectedEventBundleImpl
            try {
                doContinue = handleEventLongRunning(
                        ((ReconnectedEventBundleImpl) events).getEventNames(),
                        data);
            } catch (ClientException e) {
                log.error(
                        "Long Running listener canceled after failed execution of main run",
                        e);
                throw e;
            } finally {
                ((ReconnectedEventBundleImpl) events).disconnect();
            }

            if (!doContinue) {
                return;
            }

            // do final-processing in a new transaction
            // a new CoreSession will be open by ReconnectedEventBundleImpl
            ReconnectedEventBundleImpl postProcessEventBundle = new ReconnectedEventBundleImpl(
                    events);
            try {
                TransactionHelper.startTransaction();
                handleEventPostprocessing(postProcessEventBundle, data);
            } catch (ClientException e) {
                log.error(
                        "Long Running listener canceled after failed execution of main run",
                        e);
                throw e;
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
                postProcessEventBundle.disconnect();
            }
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
     * @throws ClientExceptions
     */
    protected abstract boolean handleEventPreprocessing(EventBundle events,
            Map<String, Object> data) throws ClientException;

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
     * @throws ClientException
     */
    protected abstract boolean handleEventLongRunning(List<String> eventNames,
            Map<String, Object> data) throws ClientException;

    /**
     * Finish processing in a dedicated Transaction
     *
     * @param events {@link EventBundle} received
     * @param data an map that may have been filled by handleEventPreprocessing
     *            and handleEventLongRunning
     * @throws ClientException
     */
    protected abstract void handleEventPostprocessing(EventBundle events,
            Map<String, Object> data) throws ClientException;

}

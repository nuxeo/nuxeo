/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.queue.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.lock.api.AlreadyLockedException;
import org.nuxeo.ecm.platform.lock.api.LockCoordinator;
import org.nuxeo.ecm.platform.lock.api.NoSuchLockException;
import org.nuxeo.ecm.platform.lock.api.NotOwnerException;
import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueException;
import org.nuxeo.ecm.platform.queue.api.QueueExecutor;
import org.nuxeo.ecm.platform.queue.api.QueueFactory;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class QueueHandlerImpl implements QueueHandler {
    public static final Log log = LogFactory.getLog(QueueHandlerImpl.class);

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.AtomicHandler#handleEndOfProcessing(
     * org.nuxeo.ecm.platform.queue.api.AtomicContent)
     */
    public void handleEndOfProcessing(QueueContent content) {

        try {
            QueueFactory factory = Framework.getLocalService(QueueFactory.class);
            QueuePersister persister = factory.getPersister(content);
            persister.forgetContent(content);
        } catch (QueueException e) {
            log.error("Couldn't remove the Queue item", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.AtomicHandler#handleNewContent(org.nuxeo
     * .ecm.platform.queue.api.AtomicContent)
     */
    public void handleNewContent(QueueContent content) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.queue.api.AtomicHandler#handleNewContentIfUnknown
     * (org.nuxeo.ecm.platform.queue.api.AtomicContent)
     */
    public void handleNewContentIfUnknown(QueueContent content)
            throws QueueException {

        URI resource;
        try {
            resource = content.getResourceURI();
        } catch (URISyntaxException e1) {
            throw new Error(e1);
        }

        QueueFactory factory = Framework.getLocalService(QueueFactory.class);
        LockCoordinator coordinator = Framework.getLocalService(LockCoordinator.class);

        QueuePersister persister = factory.getPersister(content);

        try {
            coordinator.lock(content.getOwner(), content.getResourceURI(),
                    content.getComments(), content.getDelay());
        } catch (AlreadyLockedException e) {
            log.debug("Already locked resource " + content.getDestination()
                    + ":" + content.getName(), e);
            return;
        } catch (Throwable e) {
            throw new QueueException("Couldn't lock the resource", e, content);
        }

        try {
            if (persister.hasContent(content)) {
                return;
            }
            persister.saveContent(content);
        } finally {
            try {
                coordinator.unlock(content.getOwner(), resource);
            } catch (NoSuchLockException e) {
                log.warn("Resource is unexpectedly not locked", e);
                return;
            } catch (NotOwnerException e) {
                log.warn("Resource is unexpectedly locked by another user", e);
                return;
            } catch (InterruptedException e) {
                log.error(
                        "Unexpected error while trying to unlock the resource",
                        e);
                throw new Error(
                        "Unexpected error while trying to unlock the resource",
                        e);

            }
        }

        QueueExecutor executor = factory.getExecutor(content);

        persister.setExecuteTime(content, new Date());
        executor.execute(content, this);

    }

}

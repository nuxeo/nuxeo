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

import java.io.Serializable;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.platform.lock.api.AlreadyLockedException;
import org.nuxeo.ecm.platform.lock.api.LockCoordinator;
import org.nuxeo.ecm.platform.lock.api.NoSuchLockException;
import org.nuxeo.ecm.platform.lock.api.NotOwnerException;
import org.nuxeo.ecm.platform.queue.api.QueueError;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.ecm.platform.queue.api.QueueInfo;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;
import org.nuxeo.ecm.platform.queue.api.QueueProcessor;
import org.nuxeo.runtime.api.Framework;

/**
 * Clustered implementation of queues, delegates coordination to lock service
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class DefaultQueueHandler implements QueueHandler {

    protected static Log log = LogFactory.getLog(DefaultQueueHandler.class);

    protected int delay;

    protected DefaultQueueRegistry registry;

    public void setDelay(int delay) {
        this.delay = delay;
    }

    protected DefaultQueueHandler(int delay, DefaultQueueRegistry registry) {
        this.delay = delay;
        this.registry = registry;
    }

    @Override
    public <C extends Serializable> void newContent(URI owner, URI name, C content) {
            if (!isServerActive()) {
                throw new QueueError("Server is not active");
            }
            // add content in queue
            log.debug("Adding " + name);
            QueuePersister<C> persister = registry.getPersister(name);
            QueueInfo<C> info = persister.addContent(owner, name, content);

            // process content
            log.debug("Processing " + name);
            QueueProcessor<C> executor = registry.getProcessor(name);
            persister.setLaunched(name);
            executor.process(info);
    }

    @Override
    public <C extends Serializable> void newContentIfUnknown(URI ownerName, URI name, C content) {
            if (!isServerActive()) {
                throw new QueueError("Server is not active");
            }

            log.debug("Locking " + name);
        LockCoordinator coordinator = Framework.getLocalService(LockCoordinator.class);

        try {
            coordinator.lock(ownerName, name, "locking for injecting  " + name , delay);
        } catch (AlreadyLockedException e) {
            log.debug("Already locked resource " +  name, e);
            return;
        } catch (Throwable e) {
            throw new QueueError("Couldn't lock the resource", e, name);
        }

        log.debug("Persisting " + name);
        QueuePersister<C> persister = registry.getPersister(name);

        QueueInfo<C> info;

        try {
            if (persister.hasContent(name)) {
                return;
            }
            info = persister.addContent(ownerName, name, content);
        } finally {
            try {
                coordinator.unlock(ownerName, name);
            } catch (NoSuchLockException e) {
                throw new QueueError("Resource is unexpectedly not locked", e);
            } catch (NotOwnerException e) {
                log.warn("Resource is unexpectedly locked by another user", e);
                return;
            }
        }

        log.debug("Processing " + name);
        QueueProcessor<C> executor = registry.getProcessor(name);
        persister.setLaunched(name);
        try {
            executor.process(info);
        } catch (Throwable e) {
            log.error("Processor throwed an error while processing "+ name);
        }
    }

    protected boolean isServerActive()  {
        AdministrativeStatusManager manager = Framework.getLocalService(AdministrativeStatusManager.class);
        return manager.getNuxeoInstanceStatus().isActive();
    }

    @Override
    public URI newName(String queueName, String contentName) {
        return registry.newContentName(queueName, contentName);
    }

    @Override
    public <C extends Serializable> QueueInfo<C> blacklist(URI name) {
        QueuePersister<C> persister = registry.getPersister(name);
        QueueInfo<C> info = persister.setBlacklisted(name);
        return info;
    }

    @Override
    public <C extends Serializable> QueueInfo<C> retry(URI contentName) {
        QueuePersister<C> persister = registry.getPersister(contentName);
        QueueInfo<C> info = persister.getInfo(contentName);
        QueueProcessor<C> processor = registry.getProcessor(contentName);
        processor.process(info);
        return info;
    }

}

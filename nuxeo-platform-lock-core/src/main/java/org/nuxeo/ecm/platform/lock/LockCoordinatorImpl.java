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
package org.nuxeo.ecm.platform.lock;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.lock.api.AlreadyLockedException;
import org.nuxeo.ecm.platform.lock.api.LockCoordinator;
import org.nuxeo.ecm.platform.lock.api.LockInfo;
import org.nuxeo.ecm.platform.lock.api.NoSuchLockException;
import org.nuxeo.ecm.platform.lock.api.NotOwnerException;

/**
 * LockCoordinator implementation.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class LockCoordinatorImpl implements LockCoordinator,
        LockComponentDelegate {

    public static final Log log = LogFactory.getLog(LockCoordinatorImpl.class);

    LockRecordProvider provider;

    public void activate(LockComponent component) {
        provider = component.provider;
    }

    public void disactivate() {

    }

    protected void debug(String message, URI self, URI resource,
            String comment, long timeout) {
        if (log.isDebugEnabled()) {
            log.debug(message + " owner: " + self + " resource: " + resource
                    + " comments: " + comment + " timeout: " + timeout);
        }
    }

    public void lock(final URI self, final URI resource, final String comment,
            final long timeout) throws AlreadyLockedException,
            InterruptedException {
        debug("Lock", self, resource, comment, timeout);
        doLock(self, resource, comment, timeout);
    }

    protected void doLock(final URI self, final URI resource,
            final String comment, final long timeout)
            throws AlreadyLockedException, InterruptedException {

        try {
            debug("Create Record", self, resource, comment, timeout);
            provider.createRecord(self, resource, comment, timeout);
        } catch (EntityExistsException exists) {
            debug("Couldn't create, entity already exists, fetching resource",
                    self, resource, comment, timeout);

            LockRecord record = doFetch(resource);

            debug("wait for existing lock timeout", self, resource, comment,
                    timeout);
            doWaitFor(record);

            debug("do update", self, resource, comment, timeout);
            doUpdate(self, resource, comment, timeout);
        }

    }

    protected void doWaitFor(LockRecord record) throws InterruptedException {
        long remaining = remaining(record);
        while (remaining > 0) {
            Thread.sleep(remaining);
            remaining = remaining(record);
        }
    }

    protected LockRecord doFetch(URI resource) throws AlreadyLockedException,
            InterruptedException {
        try {
            return provider.getRecord(resource);
        } catch (EntityNotFoundException notfound) {
            throw new AlreadyLockedException(resource);
        }
    }

    protected void doUpdate(URI self, URI resource, String comment, long timeout)
            throws AlreadyLockedException, InterruptedException {
        try {
            provider.updateRecord(self, resource, comment, timeout);
        } catch (OptimisticLockException e) {
            debug("doUpdate: concurent access detected", self, resource,
                    comment, timeout);
            log.debug("Concurent access detected, trying relocking", e);
            doLock(self, resource, comment, timeout);
        } catch (NoResultException e) {
            throw new AlreadyLockedException(resource);
        } catch (Throwable e) {
            log.warn("Unexpected problem while updating", e);
            throw new Error("Unexpected problem while updating " + resource, e);
        }

    }

    private long remaining(LockRecord record) {
        long now = new Date().getTime();
        long remaining = record.expireTime.getTime() - now;
        return remaining;
    }

    public LockInfo getInfo(final URI resource) throws NoSuchLockException,
            InterruptedException {

        LockRecord record = provider.getRecord(resource);
        return new LockInfoImpl(record);

    }

    public void saveInfo(URI self, URI resource, Serializable info)
            throws NotOwnerException, InterruptedException {
        LockRecord record = provider.getRecord(resource);
        if (!self.equals(record.owner)) {
            throw new NotOwnerException(resource);
        }
        record.info = info;
    }

    public void unlock(URI self, URI resource) throws NoSuchLockException,
            NotOwnerException, InterruptedException {

        LockRecord record;
        // entity there ?
        try {
            record = provider.getRecord(resource);
        } catch (NoResultException e) {
            throw new NoSuchLockException(e, resource);
        }
        // same owner ?
        if (!self.equals(record.owner)) {
            throw new NotOwnerException(resource);
        }

        try {
            provider.delete(resource);
        } catch (EntityNotFoundException notfound) {
            throw new NoSuchLockException(notfound, resource);
        }

    }

}

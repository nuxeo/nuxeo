/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Contributors: Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.lock;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implementation of the lockRecordProvider that is running a
 * JPALockRecordProvider in its single Thread.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class ThreadedLockRecordProvider implements LockRecordProvider,
        LockComponentDelegate {

    ExecutorService service;

    JPALockRecordProvider delegate;

    public void activate(LockComponent component) {
        delegate = new JPALockRecordProvider();
        delegate.activate(component);
        service = Executors.newSingleThreadExecutor();
    }

    public void disactivate() {
        delegate.disactivate();
        service.shutdown();
        delegate = null;
        service = null;
    }

    public LockRecord createRecord(final URI self, final URI resource,
            final String comment, final long timeout)
            throws InterruptedException {
        Future<LockRecord> future = service.submit(new Callable<LockRecord>() {

            public LockRecord call() throws Exception {
                return delegate.createRecord(self, resource, comment, timeout);
            }
        });
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new Error("unexpected error from provider", e);
        }
    }

    public void delete(final URI resource) throws InterruptedException {
        Future<Object> future = service.submit(Executors.callable(new Runnable() {
            public void run() {
                delegate.delete(resource);
            }
        }));
        try {
            future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new Error("unexpected error from provider", e);
        }
    }

    public LockRecord getRecord(final URI resourceUri)
            throws InterruptedException {
        Future<LockRecord> future = service.submit(new Callable<LockRecord>() {
            public LockRecord call() throws Exception {
                return delegate.getRecord(resourceUri);
            }
        });
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new Error("unexpected error from provider", e);
        }
    }

    public LockRecord updateRecord(final URI self, final URI resource,
            final String comments, final long timeout)
            throws InterruptedException {
        Future<LockRecord> future = service.submit(new Callable<LockRecord>() {
            public LockRecord call() throws Exception {
                return delegate.updateRecord(self, resource, comments, timeout);
            }
        });
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new Error("unexpected error from provider", e);
        }
    }

}

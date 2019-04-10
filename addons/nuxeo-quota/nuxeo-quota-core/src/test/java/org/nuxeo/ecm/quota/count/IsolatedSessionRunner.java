/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.quota.count;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.TransactionalCoreSessionWrapper;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author dmetzler
 *
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
        dispose(session);
        this.eventService = eventService;
    }

    /**
     * @param runnable
     * @throws Exception
     */
    public void run(RunnableWithException runnable) throws Exception {
        TransactionHelper.startTransaction();

        try {
            runnable.run();
            session.save();
        } finally {

            TransactionHelper.commitOrRollbackTransaction();
            Thread.sleep(getWaitTimeInMs());
            eventService.waitForAsyncCompletion();
            dispose(session);
        }

    }

    protected void dispose(CoreSession session) throws Exception {
        if (Proxy.isProxyClass(session.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(session);
            if (handler instanceof TransactionalCoreSessionWrapper) {
                Field field = TransactionalCoreSessionWrapper.class.getDeclaredField("session");
                field.setAccessible(true);
                session = (CoreSession) field.get(handler);
            }
        }
        if (!(session instanceof LocalSession)) {
            throw new UnsupportedOperationException(
                    "Cannot dispose session of class " + session.getClass());
        }
        ((LocalSession) session).getSession().dispose();
    }

    public long getWaitTimeInMs() {
        return waitTimeInMs;
    }

    public void setWaitTimeInMs(int waitTimeInMs) {
        this.waitTimeInMs = waitTimeInMs;
    }

}

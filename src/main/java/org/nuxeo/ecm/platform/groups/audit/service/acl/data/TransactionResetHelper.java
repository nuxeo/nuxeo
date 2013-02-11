/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stéphane Lacoin
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.TransactionalCoreSessionWrapper;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author slacoin@nuxeo.com
 */
public class TransactionResetHelper {
    protected static Field handlerField = getHandlerField();

    public static void resetTransaction(CoreSession session) {
        try {
            TransactionHelper.commitOrRollbackTransaction();
            unwrapSession(session).dispose();
            TransactionHelper.startTransaction();
            session.getRootDocument();
        } catch (ClientException e) {
            throw new ClientRuntimeException(
                    "Cannot reset transaction context", e);
        }
    }

    public static Session unwrapSession(CoreSession session)
            throws ClientException {
        if (Proxy.isProxyClass(session.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(session);
            if (handler instanceof TransactionalCoreSessionWrapper) {
                if (handlerField != null) {
                    try {
                        session = (CoreSession) handlerField.get(handler);
                    } catch (IllegalAccessException e) {
                        ;
                    }
                }
            }
        }
        if (!(session instanceof LocalSession)) {
            throw new ClientException("Not a local session "
                    + session.getClass());
        }
        return ((LocalSession) session).getSession();
    }

    public static Field getHandlerField() {
        Field field;
        try {
            field = TransactionalCoreSessionWrapper.class.getDeclaredField("session");
        } catch (Exception e) {
            return null;
        }
        field.setAccessible(true);
        return field;
    }
}

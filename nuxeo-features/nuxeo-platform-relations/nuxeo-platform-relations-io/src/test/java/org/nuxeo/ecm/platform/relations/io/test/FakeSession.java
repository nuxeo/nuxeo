/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FakeSession.java 25081 2007-09-18 14:57:22Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.io.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.nuxeo.ecm.core.model.Session;

/**
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 */
public class FakeSession {

    protected static class SessionInvocationHandler implements
            InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            final String name = method.getName();
            if (name.equals("getDocumentByUUID")) {
                return new FakeDocument((String) args[0]);
            } else if (name.equals("isLive")) {
                return Boolean.TRUE;
            }
            return null;
        }
    }

    public static Session getSession() {
        return (Session) Proxy.newProxyInstance(Session.class.getClassLoader(),
                new Class<?>[] { Session.class },
                new SessionInvocationHandler());
    }

}

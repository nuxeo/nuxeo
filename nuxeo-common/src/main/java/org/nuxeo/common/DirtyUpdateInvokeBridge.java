/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.common;

import java.util.Calendar;

/**
 * This class is used for transmitting dirty tag context on server and client
 * side from EJB invokes to the core API (NXP-4914).
 * <p>
 * Core API is loaded in a
 * separate class loader and cannot be accessed by the interceptor. In any
 * context, nuxeo common classes are always accessible by any class loaders.
 * This is the only place identified for putting that kind of logic without
 * modifying the server assemblies.
 *
 * @author matic
 */
public class DirtyUpdateInvokeBridge {

    protected static ThreadLocal<ThreadContext> contextHolder = new ThreadLocal<ThreadContext>();

    public static class ThreadContext {
        public final Long tag;

        public final Long invoked;
        ThreadContext(Long tag) {
            this.tag = tag;
            this.invoked = Calendar.getInstance().getTimeInMillis();
        }
    }

    public static void putTagInThreadContext(Object tag) {
        contextHolder.set(new ThreadContext((Long) tag));
    }

    public static void clearThreadContext() {
        contextHolder.remove();
    }

    public static ThreadContext getThreadContext() {
            return contextHolder.get();
    }

}

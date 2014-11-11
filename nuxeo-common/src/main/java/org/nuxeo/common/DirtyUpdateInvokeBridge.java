/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

    protected static final ThreadLocal<ThreadContext> contextHolder = new ThreadLocal<ThreadContext>();

    private DirtyUpdateInvokeBridge() {
    }

    public static class ThreadContext {
        public final Long tag;

        public final Long invoked;

        ThreadContext(Long tag) {
            this.tag = tag;
            invoked = Calendar.getInstance().getTimeInMillis();
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

package org.nuxeo.common;

import java.util.Calendar;

/**
 * This class is used for transmitting dirty tag context on server and client
 * side from EJB invokes to the core API (NXP-4914). Core API is loaded in a
 * separate class loader and cannot be accessed by the interceptor. In any
 * context, nuxeo common classes are always accessible by any class loaders.
 * This is the only place identified for putting that kind of logic without
 * modifying the server assemblies.
 * 
 * @author matic
 * 
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

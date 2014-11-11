package org.nuxeo.core.jboss.interceptors;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.nuxeo.common.DirtyUpdateInvokeBridge;

public class DirtyUpdateServerInterceptor implements Interceptor, DirtyUpdateConstants {

    public String getName() {
        return getClass().getSimpleName();
    }

    public Object invoke(Invocation invocation) throws Throwable {
        Object tag = invocation.getMetaData(TAG, ATTRIBUTE);
        if (tag == null) { // protect from local invoke
            return invocation.invokeNext();
        }
        try {
            DirtyUpdateInvokeBridge.putTagInThreadContext(tag);
            return invocation.invokeNext();
        } finally {
            DirtyUpdateInvokeBridge.clearThreadContext();
        }
    }
}

package org.nuxeo.runtime.jboss.interceptors;

import org.javasimon.Split;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;

public class ServerMonitoringInterceptor extends AbstractMonitoring implements Interceptor {

    public String getName() {
        return getClass().getCanonicalName();
    }

    protected ThreadLocal<Split> reentrancy = new ThreadLocal<Split>();

    public Object invoke(Invocation context) throws Throwable {
        Split split = reentrancy.get();
        if (!(context instanceof MethodInvocation) || split != null) {
            return context.invokeNext();
        }
        MethodInvocation mContext = (MethodInvocation)context;
        split = getStopwatch(mContext).start();
        reentrancy.set(split);
        try {
            return context.invokeNext();
        } finally {
            context.addResponseAttachment(RESPONSE_DURATION_TAG, Long.valueOf(split.stop()));
            reentrancy.remove();
        }
    }

}

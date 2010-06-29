package org.nuxeo.runtime.jboss.interceptors;

import org.javasimon.Counter;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.aop.joinpoint.MethodInvocation;

public class ClientMonitoringInterceptor extends AbstractMonitoring implements org.jboss.aop.advice.Interceptor, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public String getName() {
        return getClass().getCanonicalName();
    }



    public Object invoke(Invocation context) throws Throwable {
        MethodInvocation mCtx = (MethodInvocation)context;;
        Stopwatch stopwatch  = getStopwatch(mCtx);
        Split split = stopwatch.start();
        try {
            return context.invokeNext();
        } finally {
            split.stop();
            Long duration = (Long) context.getResponseAttachment(RESPONSE_DURATION_TAG);
            Counter counter = getCounter(mCtx,"serverDuration");
            counter.set(duration == null ? 0 : duration.longValue());
        }
    }

}

package org.nuxeo.ecm.core.api.ejb;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;

public class ReentrantCallsShieldInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception
    {
        try {
            DocumentModelImpl.reentrantCoreSession.set((CoreSession) ctx.getTarget());
            return ctx.proceed();
        } finally {
            DocumentModelImpl.reentrantCoreSession.set(null);
        }
    }

}

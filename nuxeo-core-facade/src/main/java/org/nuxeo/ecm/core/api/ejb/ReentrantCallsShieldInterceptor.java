package org.nuxeo.ecm.core.api.ejb;

import javax.ejb.ConcurrentAccessException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;

/**
 * This interceptor is used to detect reentrant calls to DocumentManagerBean.
 * In case of reentrancy it store the local CoreSession instance in a thread local that is must be used
 * by all the code using CoreSession withing the scope of the current call.
 *
 * Typically, AbstractSession, EventHandlers and DocumentModelImpl may use the CoreSession while current thread
 * is processing a call to DocumentManagerBean.
 * Thanks to the threadlocal Local Session, the call can be nested without raising the {@link ConcurrentAccessException}
 *
 *
 * @author tiry
 *
 */
public class ReentrantCallsShieldInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception
    {
        String sid = null;
        try {
            CoreSession targetSession = (CoreSession) ctx.getTarget();
            sid = targetSession.getSessionId();
            DocumentModelImpl.reentrantCoreSession.get().put(sid, targetSession);
            return ctx.proceed();
        } finally {
            if (sid!=null) {
                DocumentModelImpl.reentrantCoreSession.get().remove(sid);
            }
        }
    }

}

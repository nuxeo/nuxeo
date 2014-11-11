package org.nuxeo.core.jboss.interceptors;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.ejb3.stateful.StatefulRemoteInvocation;
import org.nuxeo.common.DirtyUpdateInvokeBridge;

public class DirtyUpdateClientInterceptor implements Interceptor, Serializable, DirtyUpdateConstants {

    private static final long serialVersionUID = 1L;

    public String getName() {
        return getClass().getSimpleName();
    }

    public final static Log log = LogFactory.getLog(DirtyUpdateClientInterceptor.class);

    protected void addDirtyTag(StatefulRemoteInvocation invocation) {
        Object tag = DirtyUpdateInvokeBridge.getThreadContext().tag;
        invocation.getMetaData().addMetaData(TAG, ATTRIBUTE, tag);
    }

    public Object invoke(Invocation invocation) throws Throwable {
        addDirtyTag((StatefulRemoteInvocation) invocation);
        return invocation.invokeNext();
    }
}

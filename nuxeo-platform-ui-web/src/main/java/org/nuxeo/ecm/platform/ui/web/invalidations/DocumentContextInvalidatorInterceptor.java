package org.nuxeo.ecm.platform.ui.web.invalidations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.intercept.Interceptor;
import org.jboss.seam.core.BijectionInterceptor;
import org.jboss.seam.intercept.AbstractInterceptor;
import org.jboss.seam.intercept.InvocationContext;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 *
 * Interceptor used for automatic injection/invalidation tied to
 * currentDocumentModel
 *
 * @author tiry
 *
 */
@Interceptor(stateless = true, within = BijectionInterceptor.class)
public class DocumentContextInvalidatorInterceptor extends AbstractInterceptor {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentContextInvalidatorInterceptor.class);

    public Object aroundInvoke(InvocationContext invocationContext)
            throws Exception {
        beforeInvocation(invocationContext);
        return invocationContext.proceed();
    }

    private void beforeInvocation(InvocationContext invocationContext) {
        Object target = invocationContext.getTarget();
        for (Method meth : target.getClass().getMethods()) {
            if (meth.isAnnotationPresent(DocumentContextInvalidation.class)) {
                try {
                    doInvalidationCall(target, meth);
                } catch (Exception e) {
                    log.error("Error during Invalidation method call", e);
                }
            }
        }
        return;
    }

    private void doInvalidationCall(Object target, Method meth)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (meth.getParameterTypes().length == 0) {
            meth.invoke(target);
        } else {
            DocumentModel currentDoc = getCurrentDocumentModel();
            if (currentDoc != null) {
                meth.invoke(target, currentDoc);
            } else {
                log.error("Unable to get CurrentDocument");
            }
        }
    }

    private DocumentModel getCurrentDocumentModel() {
        NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", ScopeType.CONVERSATION);
        return navigationContext.getCurrentDocument();
    }

}

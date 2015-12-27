/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.invalidations;

import java.lang.reflect.Method;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.intercept.Interceptor;
import org.jboss.seam.core.BijectionInterceptor;
import org.jboss.seam.intercept.AbstractInterceptor;
import org.jboss.seam.intercept.InvocationContext;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 * Interceptor used for automatic injection/invalidation tied to currentDocumentModel
 *
 * @author tiry
 */
@Interceptor(stateless = true, within = BijectionInterceptor.class)
public class DocumentContextInvalidatorInterceptor extends AbstractInterceptor {

    private static final long serialVersionUID = 1L;

    @Override
    public Object aroundInvoke(InvocationContext invocationContext) throws Exception { // stupid Seam API
        beforeInvocation(invocationContext);
        return invocationContext.proceed();
    }

    private void beforeInvocation(InvocationContext invocationContext) {
        Object target = invocationContext.getTarget();
        for (Method meth : target.getClass().getMethods()) {
            if (meth.isAnnotationPresent(DocumentContextInvalidation.class)) {
                doInvalidationCall(target, meth);
            }
        }
    }

    private void doInvalidationCall(Object target, Method meth) {
        try {
            if (meth.getParameterTypes().length == 0) {
                meth.invoke(target);
            } else {
                DocumentModel currentDoc = getCurrentDocumentModel();
                // currentDoc may be null, it's ok
                meth.invoke(target, currentDoc);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private DocumentModel getCurrentDocumentModel() {
        NavigationContext navigationContext = (NavigationContext) Component.getInstance("navigationContext",
                ScopeType.CONVERSATION);
        return navigationContext.getCurrentDocument();
    }

    @Override
    public boolean isInterceptorEnabled() {
        return true;
    }

}

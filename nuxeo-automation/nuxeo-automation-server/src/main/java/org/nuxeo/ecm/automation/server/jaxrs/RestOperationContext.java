/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;

/**
 * A custom operation context to be used in REST calls on server side. This
 * implementation is delegating the post execution cleanup to the webengine
 * filter through {@link RequestContext} and {@link RequestCleanupHandler}.
 * <p>
 * This way temporary resources like files used by operations are removed after
 * the response is sent to the client.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RestOperationContext extends OperationContext {

    private static final long serialVersionUID = 1L;

    /**
     * Must be called before context execution.
     */
    public void addRequestCleanupHandler(HttpServletRequest request) {
        RequestContext.getActiveContext(request).addRequestCleanupHandler(
                new RequestCleanupHandler() {
                    public void cleanup(HttpServletRequest req) {
                        try {
                            deferredDispose();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    protected void deferredDispose() throws OperationException {
        super.dispose();
    }

    @Override
    public void dispose() {
        // do nothing
    }

}

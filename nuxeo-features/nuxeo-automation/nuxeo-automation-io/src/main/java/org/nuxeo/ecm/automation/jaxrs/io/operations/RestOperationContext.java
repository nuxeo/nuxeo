/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;

/**
 * A custom operation context to be used in REST calls on server side. This implementation is delegating the post
 * execution cleanup to the webengine filter through {@link RequestContext} and {@link RequestCleanupHandler}.
 * <p>
 * This way temporary resources like files used by operations are removed after the response is sent to the client.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RestOperationContext extends OperationContext {


    /**
     * Specify the http status when no failure occurs.
     *
     * @since 7.1
     */
    protected int httpStatus = HttpServletResponse.SC_OK;

    /**
     * Must be called before context execution.
     */
    public void addRequestCleanupHandler(HttpServletRequest request) {
        RequestContext.getActiveContext(request).addRequestCleanupHandler(new RequestCleanupHandler() {
            @Override
            public void cleanup(HttpServletRequest req) {
                try {
                    deferredDispose();
                } catch (OperationException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * @since 7.1
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * @since 7.1
     */
    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    protected void deferredDispose() throws OperationException {
        super.dispose();
    }

    @Override
    public void dispose() {
        // do nothing
    }

}

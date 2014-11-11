/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.webengine.session.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.session.UserSession;

/**
 * A custom operation context to be used in REST calls on server side. This
 * implementation is delegating the post execution cleanup to the webengine
 * filter through {@link UserSession} and {@link RequestCleanupHandler}.
 * <p>
 * This way temporary resources like files used by operations are removed after
 * the response is sent to the client.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RestOperationContext extends OperationContext {

    private static final long serialVersionUID = 1L;

    /**
     * Must be called before context execution
     */
    public void addRequestCleanupHandler(HttpServletRequest request) {
        UserSession.addRequestCleanupHandler(request,
                new RequestCleanupHandler() {
                    public void cleanup(HttpServletRequest req) {
                        deferredDispose();
                    }
                });
    }

    protected void deferredDispose() {
        super.dispose();
    }

    @Override
    public void dispose() {
        // do nothing
    }

}

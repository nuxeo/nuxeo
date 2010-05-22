/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.automation.client.jaxrs.impl;

import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.nuxeo.ecm.automation.client.jaxrs.spi.AbstractAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.spi.DefaultSession;

/**
 * Need to have different cookie stores for different sessions
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SessionImpl extends DefaultSession {

    protected BasicHttpContext ctx;

    public SessionImpl(AbstractAutomationClient client, String username, String password) {
        super (client, username, password);
        ctx = new BasicHttpContext();
        ctx.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
    }

    public BasicHttpContext getContext() {
        return ctx;
    }

}

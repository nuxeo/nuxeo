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
 *     slacoin
 */
package org.nuxeo.ecm.automation.client.jaxrs;

import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.BasicAuthInterceptor;
import org.nuxeo.ecm.automation.client.jaxrs.spi.auth.PortalSSOAuthInterceptor;

/**
 * Provide a way of intercepting requests before they are sent
 * server side. Authentication headers are injected this way.
 *
 * @see BasicAuthInterceptor
 * @see PortalSSOAuthInterceptor
 *
 */
public interface RequestInterceptor {

    void processRequest(Request request, Connector connector);

}

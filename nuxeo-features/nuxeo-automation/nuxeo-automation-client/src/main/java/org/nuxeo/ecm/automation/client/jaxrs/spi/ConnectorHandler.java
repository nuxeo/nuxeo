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
package org.nuxeo.ecm.automation.client.jaxrs.spi;


public class ConnectorHandler implements Connector {

    protected final Connector connector;

    protected final RequestInterceptor interceptor;

    public ConnectorHandler(Connector connector, RequestInterceptor interceptor) {
       this.connector = connector;
       this.interceptor = interceptor;
    }

    @Override

    public Object execute(Request request) {
        interceptor.processRequest(request, connector);
        return connector.execute(request);
    }

}

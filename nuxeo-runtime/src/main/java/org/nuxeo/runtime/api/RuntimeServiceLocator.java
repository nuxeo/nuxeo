/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.util.Properties;

/**
 * A server implementation that use Nuxeo Runtime to lookup services.
 * <p>
 * This is used as the default server if no other is specified.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RuntimeServiceLocator implements ServiceLocator {

    private static final long serialVersionUID = -3550824536420353831L;

    @Override
    public void initialize(String host, int port, Properties properties)
            throws Exception {
        // do nothing
    }

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public Object lookup(ServiceDescriptor svc) throws Exception {
        return Framework.getLocalService(svc.getServiceClass());
    }

    @Override
    public Object lookup(String serviceId) throws Exception { //TODO
        throw new UnsupportedOperationException("not yet implemented");
    }

}

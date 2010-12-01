/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

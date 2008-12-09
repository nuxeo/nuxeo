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

package org.nuxeo.ecm.platform.api;

import javax.naming.NamingException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated use new service API {@link org.nuxeo.runtime.api.ServiceManagement}
 */
@Deprecated
public class DefaultServiceConnector implements ServiceConnector {

    private static final long serialVersionUID = -8049845085311646878L;

    private static final ServiceConnector INSTANCE = new DefaultServiceConnector();

    public static ServiceConnector getInstance() {
        return INSTANCE;
    }

    public Object connect(ServiceDescriptor sd) throws NamingException {
        return sd.server.getJndiContext().lookup(sd.jndiName);
    }

}

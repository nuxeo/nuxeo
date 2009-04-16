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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceManagement;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated use new service API {@link ServiceManagement}
 */
@Deprecated
public class DefaultRepositoryConnector implements RepositoryConnector {

    private static final long serialVersionUID = 3680869800536175343L;

    private static final RepositoryConnector INSTANCE = new DefaultRepositoryConnector();


    public static RepositoryConnector getInstance() {
        return INSTANCE;
    }

    public CoreSession connect(RepositoryDescriptor rd) throws NamingException {
        ServiceDescriptor sd = rd.server.getServiceDescriptor(CoreSession.class.getName());
        if (sd != null) {
            return (CoreSession) rd.server.getService(sd);
        }
        return Framework.getLocalService(CoreSession.class);
    }

}

/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core;

import java.util.Properties;

import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.runtime.api.ServiceDescriptor;
import org.nuxeo.runtime.api.ServiceLocator;

public class LocalRepositoryLocator implements ServiceLocator {

    private static final long serialVersionUID = 6771513926490100253L;

    public void dispose() {
        // TODO Auto-generated method stub
    }

    public void initialize(String host, int port, Properties properties)
            throws Exception {
        // TODO Auto-generated method stub
    }

    public Object lookup(ServiceDescriptor descriptor) throws Exception {
        return new LocalSession();
    }

    public Object lookup(String serviceId) throws Exception {
        return new LocalSession();
    }

}

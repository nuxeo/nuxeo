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

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.api.ServiceAdapter;
import org.nuxeo.runtime.api.ServiceDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CoreSessionAdapter implements ServiceAdapter {

    public Object adapt(ServiceDescriptor svc, Object service) throws Exception {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        CoreSession session = (CoreSession) service;
        String sid = session.connect(svc.getName(), ctx);
        // register session on local JVM so it can be used later by doc models
        CoreInstance.getInstance().registerSession(sid, session);
        return session;
    }

}

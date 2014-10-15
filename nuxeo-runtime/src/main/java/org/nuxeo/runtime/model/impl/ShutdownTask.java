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
package org.nuxeo.runtime.model.impl;

import java.util.Set;

import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * Deactivate components in the proper order to avoid exceptions at shutdown.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class ShutdownTask {

    final static void shutdown(ComponentManagerImpl mgr) {
        RegistrationInfoImpl[] ris = mgr.reg.getComponentsArray();
        for (RegistrationInfoImpl ri : ris) {
            shutdown(mgr, ri);
        }
    }

    private static void shutdown(ComponentManagerImpl mgr,
            RegistrationInfoImpl ri) {
        ComponentName name = ri.getName();
        if (name == null) {
            return; // already destroyed
        }
        if (ri.getState() <= RegistrationInfo.RESOLVED) {
            // not yet activated so we can destroy it right now
            mgr.unregister(name);
            return;
        }
        // an active component - get the components depending on it
        Set<ComponentName> reqs = mgr.reg.requirements.get(name);
        if (reqs != null && !reqs.isEmpty()) {
            // there are some components depending on me - cannot shutdown
            for (ComponentName req : reqs.toArray(new ComponentName[reqs.size()])) {
                RegistrationInfoImpl parentRi = mgr.reg.components.get(req);
                if (parentRi != null) {
                    shutdown(mgr, parentRi);
                }
            }
        }
        // no components are depending on me - shutdown now
        mgr.unregister(name);
    }

}

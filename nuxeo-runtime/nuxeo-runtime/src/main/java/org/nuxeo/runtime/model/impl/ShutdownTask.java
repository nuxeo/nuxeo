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

import org.nuxeo.runtime.model.ComponentName;

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
        mgr.unregister(name);
    }

}

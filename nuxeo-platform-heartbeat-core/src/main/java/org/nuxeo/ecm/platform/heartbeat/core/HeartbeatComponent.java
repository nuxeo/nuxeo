/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.heartbeat.core;

import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatManager;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Framework for accessing to running nuxeo server in a cluster using heartbeat
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
public class HeartbeatComponent extends DefaultComponent {

    protected static HeartbeatComponent defaultComponent;

    protected DocumentHeartbeatManager manager;

    @Override
    public void activate(ComponentContext context) throws Exception {
        defaultComponent = this;
        manager = new DocumentHeartbeatManager();
        // heart-beat is started once the document store is initialized (HeartbeatInitializationHandler)
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        manager.stop();
        manager = null;
        defaultComponent = null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (HeartbeatManager.class.isAssignableFrom(adapter)) {
            return adapter.cast(manager);
        }
        return super.getAdapter(adapter);
    }

}

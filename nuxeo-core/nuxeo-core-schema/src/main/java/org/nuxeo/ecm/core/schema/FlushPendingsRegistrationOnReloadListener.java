/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.core.schema;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * @since 5.7
 */
public class FlushPendingsRegistrationOnReloadListener implements EventListener {

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return "reload".equals(event.getId());
    }

    @Override
    public void handleEvent(Event event) {
        if (!"reload".equals(event.getId())) {
            return;
        }
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        ((SchemaManagerImpl) mgr).flushPendingsRegistration();
    }

}

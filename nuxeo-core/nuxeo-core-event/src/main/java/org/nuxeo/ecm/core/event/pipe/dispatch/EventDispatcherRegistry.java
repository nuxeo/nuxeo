/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe.dispatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple stacking descriptor registry
 *
 * @since 8.4
 */
public class EventDispatcherRegistry {

    protected List<EventDispatcherDescriptor> contribs = new ArrayList<EventDispatcherDescriptor>();

    public void addContrib(EventDispatcherDescriptor contrib) {
        contribs.add(contrib);
    }

    public void removeContrib(EventDispatcherDescriptor contrib) {
        Iterator<EventDispatcherDescriptor> it = contribs.iterator();
        while (it.hasNext()) {
            EventDispatcherDescriptor desc = it.next();
            if (desc.getName().equals(contrib.getName())) {
                it.remove();
                break;
            }
        }
    }

    public EventDispatcherDescriptor getDispatcherDescriptor() {

        int nbd = contribs.size();
        if (nbd > 0) {
            return contribs.get(nbd - 1);
        } else {
            return null;
        }
    }
}

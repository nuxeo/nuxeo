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

package org.nuxeo.runtime.services.event;

import java.util.Arrays;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("listener")
public class ListenerDescriptor {

    private static final NullListener NULL_LISTENER = new NullListener();

    @XNodeList(value = "topic", type = String[].class, componentType = String.class)
    String[] topics;

    EventListener listener;


    @XNode("@class")
    public void setListener(Class<EventListener> listenerClass) {
        try {
            listener = listenerClass.newInstance();
        } catch (Exception e) {
            listener = NULL_LISTENER;
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return listener + " { " + Arrays.toString(topics) + " }";
    }

}

class NullListener implements EventListener {

    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    public void handleEvent(Event event) {
    }

}

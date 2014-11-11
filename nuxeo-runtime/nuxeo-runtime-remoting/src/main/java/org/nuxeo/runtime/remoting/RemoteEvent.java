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

package org.nuxeo.runtime.remoting;

import java.io.Serializable;

import org.nuxeo.runtime.model.ComponentName;


/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RemoteEvent implements Serializable {

    // the server was registered into a remote server
    public static final int COMPONENTS_ADDED = 1;
    public static final int COMPONENTS_REMOVED = 2;
    public static final int EXTENSION_ADDED = 3;
    public static final int EXTENSION_REMOVED = 4;

    private static final long serialVersionUID = -6392209237419763851L;

    // TODO: make private and final (needs some refactoring in RemotingService).
    public final int id;
    public final ComponentName component;
    public final Serializable data;

    public RemoteEvent(int eventId, ComponentName component, Serializable data) {
        id = eventId;
        this.component = component;
        this.data = data;
    }

    /**
     * @return the data.
     */
    public Object getData() {
        return data;
    }

    /**
     * @return the id.
     */
    public int getId() {
        return id;
    }

    /**
     * @return the component.
     */
    public ComponentName getComponent() {
        return component;
    }

    @Override
    public String toString() {
        return "Remote event: " + id + "; component: " + component + "; data: " + data;
    }

}

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

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RemoteComponent {

    private final Set<Extension> extensions;
    private final ServerDescriptor sd;
    private final ComponentName name;
    private RemoteContext context;

    public RemoteComponent(ServerDescriptor sd, ComponentName name) {
        this.sd = sd;
        this.name = name;
        extensions = new HashSet<Extension>();
    }

    public RemoteContext getContext() {
        if (context == null) {
            context = new RemoteContext(sd, name, null);
        }
        return context;
    }

    public ComponentName getName() {
        return name;
    }

    public void addExtension(Extension extension) {
        extensions.add(extension);
    }

    public Extension[] getExtensions() {
        return extensions.toArray(new Extension[extensions.size()]);
    }

}

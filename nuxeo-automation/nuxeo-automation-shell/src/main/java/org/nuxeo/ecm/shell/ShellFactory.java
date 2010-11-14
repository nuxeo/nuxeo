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
package org.nuxeo.ecm.shell;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public abstract class ShellFactory<T extends Shell> {

    protected Set<String> caps;

    public ShellFactory(String... caps) {
        if (caps == null || caps.length == 0) {
            throw new IllegalArgumentException(
                    "A shell factory must have at least one capability");
        }
        this.caps = new HashSet<String>();
        for (String cap : caps) {
            this.caps.add(cap);
        }
    }

    public boolean hasCapabilities(String... capabilities) {
        for (String cap : capabilities) {
            if (!caps.contains(cap)) {
                return false;
            }
        }
        return true;
    }

    public abstract T getShell();
}

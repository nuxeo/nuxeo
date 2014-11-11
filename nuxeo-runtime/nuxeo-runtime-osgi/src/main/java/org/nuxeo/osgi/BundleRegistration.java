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

package org.nuxeo.osgi;

import java.util.HashSet;
import java.util.Set;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleRegistration {

    BundleImpl bundle;

    // XXX: explain why these three variables are lazily instantiated.
    Set<String> dependsOn;
    Set<String> dependsOnMe;
    Set<String> waitingFor;

    public BundleRegistration(BundleImpl bundle) {
        this.bundle = bundle;
    }

    public void addDependency(String name) {
        if (dependsOn == null) {
            dependsOn = new HashSet<String>();
        }
        dependsOn.add(name);
    }

    public void addDependent(String name) {
        if (dependsOnMe == null) {
            dependsOnMe = new HashSet<String>();
        }
        dependsOnMe.add(name);
    }

    public void addUnresolvedDependency(String name) {
        if (waitingFor == null) {
            waitingFor = new HashSet<String>();
        }
        waitingFor.add(name);
    }

    public void removeUnresolvedDependency(String name) {
        if (waitingFor == null) {
            return;
        }
        waitingFor.remove(name);
        if (waitingFor.isEmpty()) {
            waitingFor = null;
        }
    }

    public boolean hasUnresolvedDependencies() {
        return waitingFor != null && !waitingFor.isEmpty();
    }

}

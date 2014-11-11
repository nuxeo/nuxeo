/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

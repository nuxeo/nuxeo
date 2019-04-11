/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BundleRegistration {

    protected final BundleImpl bundle;

    // XXX: explain why these three variables are lazily instantiated.
    protected final Set<String> dependsOn = new HashSet<>();

    protected final Set<String> dependsOnMe = new HashSet<>();

    protected final Set<String> waitingFor = new HashSet<>();

    protected final Set<String> extendsMe = new HashSet<>();

    public BundleRegistration(BundleImpl bundle) {
        this.bundle = bundle;
    }

    public void addFragment(String name) {
        extendsMe.add(name);
    }

    public void addDependency(String name) {
        dependsOn.add(name);
    }

    public void addDependent(String name) {
        dependsOnMe.add(name);
    }

    public void addUnresolvedDependency(String name) {
        waitingFor.add(name);
    }

    public void removeUnresolvedDependency(String name) {
        waitingFor.remove(name);
    }

    public boolean hasUnresolvedDependencies() {
        return !waitingFor.isEmpty();
    }

}

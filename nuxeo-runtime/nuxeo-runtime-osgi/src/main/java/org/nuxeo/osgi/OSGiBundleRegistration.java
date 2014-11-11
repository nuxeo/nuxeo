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

import org.osgi.framework.Bundle;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiBundleRegistration {

    protected final OSGiBundle bundle;

    protected final Set<String> pendings = new HashSet<String>();
    protected final Set<String> waitingFor = new HashSet<String>();

    protected final Set<OSGiBundleRegistration> resolvedDependencies = new HashSet<OSGiBundleRegistration>();
    protected final Set<OSGiBundleRegistration> dependsOnMe = new HashSet<OSGiBundleRegistration>();

    protected final Set<Bundle> nested = new HashSet<Bundle>();

    protected OSGiBundleRegistration(OSGiBundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public String toString() {
        return "OSGiBundleRegistration ["+ bundle + "]";
    }

}

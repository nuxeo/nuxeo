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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.deploy;

import java.util.Collection;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class Contribution implements Cloneable {

    protected Extension extension;

    protected String contributionId;

    public abstract void install(ManagedComponent comp, Contribution contrib);

    public abstract void uninstall(ManagedComponent comp, Contribution contrib);

    public String getContributionId() {
        return contributionId;
    }

    public void setContributionId(String contributionId) {
        this.contributionId = contributionId;
    }

    public void install(ManagedComponent comp) {
        install(comp, this);
    }

    public void uninstall(ManagedComponent comp) {
        uninstall(comp, this);
    }

    public void resolve(ContributionManager mgr) {
        // do noting
    }

    public void unresolve(ContributionManager mgr) {
        // do nothing
    }

    public Extension getExtension() {
        return extension;
    }

    public void setExtension(Extension extension) {
        this.extension = extension;
    }

    public String getExtensionPoint() {
        return extension.getExtensionPoint();
    }

    public ComponentInstance getContributor() {
        return extension.getComponent();
    }

    public Collection<String> getDependencies() {
        return null;
    }

    @Override
    public String toString() {
        return contributionId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Contribution) {
            return getClass() == obj.getClass() && contributionId.equals(((Contribution) obj).contributionId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return contributionId != null ? contributionId.hashCode() : 0;
    }

}

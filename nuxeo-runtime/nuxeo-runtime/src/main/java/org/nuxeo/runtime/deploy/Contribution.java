/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public abstract void install(ManagedComponent comp, Contribution contrib) throws Exception;

    public abstract void uninstall(ManagedComponent comp, Contribution contrib) throws Exception;


    public String getContributionId() {
        return contributionId;
    }

    public void setContributionId(String contributionId) {
        this.contributionId = contributionId;
    }

    public void install(ManagedComponent comp) throws Exception {
        install(comp, this);
    }

    public void uninstall(ManagedComponent comp) throws Exception {
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
            return getClass() == obj.getClass()
                    && contributionId.equals(((Contribution) obj).contributionId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return contributionId != null ? contributionId.hashCode() : 0;
    }

}

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

package org.nuxeo.ecm.webengine.config;

import java.util.Collection;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class Contribution implements Cloneable {

    protected String id;
    protected Extension extension;


    public abstract void install(Object obj, Contribution contrib) throws Exception;

    public abstract void uninstall(Object obj, Contribution contrib) throws Exception;

    public void install(Object obj) throws Exception {
        install(obj, this);
    }

    public void uninstall(Object obj) throws Exception {
        uninstall(obj, this);
    }


    public void resolve(ContributionManager mgr) {
        // extend this
    }

    public void unresolve(ContributionManager mgr) {
        // extend this
    }

    public String getId() {
        return id;
    }

    /**
     * @return the extension.
     */
    public Extension getExtension() {
        return extension;
    }

    /**
     * @param extension the extension to set.
     */
    public void setExtension(Extension extension) {
        this.extension = extension;
    }

    /**
     * @return the extensionPoint.
     */
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
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Contribution) {
            return this.getClass() == obj.getClass() && id.equals(((Contribution)obj).id);
        }
        return false;
    }

}

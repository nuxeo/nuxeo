/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for the {@code factory} elements of the {@code activeFileSystemItemFactories} contributions.
 * 
 * @author Antoine Taillefer
 */
@XObject("factory")
public class ActiveFileSystemItemFactoryDescriptor implements Serializable {

    private static final long serialVersionUID = 1630714012089221788L;

    @XNode("")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append(" (enabled = ");
        sb.append(enabled);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ActiveFileSystemItemFactoryDescriptor)) {
            return false;
        }
        return this.name.equals(((ActiveFileSystemItemFactoryDescriptor) obj).getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}

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
 *     Razvan Caraghin
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.util;

import java.io.Serializable;

/**
 * Represents a repository location. TODO: move to another package.
 *
 * @author Razvan Caraghin
 * @author Thierry Delprat
 * @author Florent Guillaume
 */
public class RepositoryLocation implements Serializable,
        Comparable<RepositoryLocation> {

    private static final long serialVersionUID = -4802281621945117577L;

    protected final String name;

    public RepositoryLocation(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null repository location");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int compareTo(RepositoryLocation o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RepositoryLocation)) {
            return false;
        }
        return name.equals(((RepositoryLocation) other).name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * @deprecated Unused
     */
    @Deprecated
    public Boolean getEnabled() {
        return Boolean.FALSE;
    }

    /**
     * @deprecated Unused
     */
    @Deprecated
    public void setEnabled(Boolean enabled) {
    }
}

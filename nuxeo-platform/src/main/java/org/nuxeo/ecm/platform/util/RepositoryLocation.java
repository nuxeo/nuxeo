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

package org.nuxeo.ecm.platform.util;

import java.io.Serializable;

/**
 * TODO: move to another package. Represents a repository location.
 *
 * @author Razvan Caraghin
 *
 */
public class RepositoryLocation implements Serializable,
        Comparable<RepositoryLocation> {

    private static final long serialVersionUID = 503609836034456298L;

    protected final String name;

    private Boolean enabled = false;

    public RepositoryLocation(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    // TODO: never called. Remove and make field final?
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    // FIXME: doesn't implement the compareTo contract.
    public int compareTo(RepositoryLocation o) {
        if (name.equalsIgnoreCase(o.name)) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RepositoryLocation)) {
            return false;
        }
        RepositoryLocation otherRepo = (RepositoryLocation) other;
        return name.equals(otherRepo.name);
    }

}

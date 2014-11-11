/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.api.login;

import java.security.Permission;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SystemLoginPermission extends Permission {

    private static final long serialVersionUID = -2587068684672935213L;

    public SystemLoginPermission() {
        super("systemLogin");
    }

    @Override
    public String getActions() {
        return "";
    }

    @Override
    public boolean implies(Permission permission) {
        return permission instanceof SystemLoginPermission;
    }

    // TODO: isn't this the default equals() implementation ?
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return obj.getClass() == SystemLoginPermission.class;
    }

    // TODO: check that this really matches the equals() implementation.
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

}

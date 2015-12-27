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

package org.nuxeo.runtime.api.login;

import java.security.Permission;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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

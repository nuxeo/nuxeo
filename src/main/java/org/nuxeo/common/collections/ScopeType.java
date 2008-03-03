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
 * $Id: ScopeType.java 19046 2007-05-21 13:03:50Z sfermigier $
 */

package org.nuxeo.common.collections;

/**
 * Scope type definitions for a scoped map.
 * <p>
 * Only request and default scopes are defined for now, but others may be added.
 *
 * @see ScopedMap
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public enum ScopeType {
    DEFAULT, REQUEST;

    public String getScopedKey(String key) {
        return getScopePrefix() + key;
    }

    public String getScopePrefix() {
        return name().toLowerCase() + '/';
    }

}

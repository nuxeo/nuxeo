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

package org.nuxeo.ecm.core.security;

import org.nuxeo.ecm.core.api.security.Access;

/**
 * @author Bogdan Stefanescu
 *
 */
public class CacheEntry {

    private final String docUid;

    private final String username;

    private final String permission;

    private final SecurityCache cache;

    private Access access;

    private int hash;

    public CacheEntry(SecurityCache cache, String docUid, String username, String permission) {
        this.docUid = docUid;
        this.username = username;
        this.permission = permission;
        this.cache = cache;
    }

    public final void setAccess(Access access) {
        this.access = access;
        if (hash == 0) {
            cache.put(this);
        }
    }

    public final Access getAccess() {
        return access;
    }

    @SuppressWarnings({"NonFinalFieldReferencedInHashCode"})
    @Override
    public int hashCode() {
        // CachEntry is immutable, we can store the computed hash code value
        int h = hash;
        if (h == 0) {
            h = 17;
            h = 37 * h + docUid.hashCode();
            h = 37 * h + username.hashCode();
            hash = 37 * h + permission.hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof CacheEntry)) {
            return false;
        }
        CacheEntry entry = (CacheEntry) other;
        return entry.docUid.equals(docUid)
            && entry.username.equals(username)
            && entry.permission.equals(permission);
    }

    @Override
    public String toString() {
        return docUid + ':' + username + ':' + permission;
    }

}

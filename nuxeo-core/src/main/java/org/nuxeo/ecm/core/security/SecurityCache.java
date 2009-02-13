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

//import java.util.Hashtable;

//import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;

// TODO: this class is not finished, and not used. (Same for CacheEntry).
/**
 * Unfinished class. Don't use.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SecurityCache {

    //private Hashtable<CacheEntry, Access> cache;

    // cacheEntry -> cacheEntry
    // user -> List {cacheEntry}
    // document -> List {cacheEntry}


    public void put(CacheEntry entry) {
        //cache.put(entry);
    }

    public CacheEntry get(Document doc, String username, String permission) {
        return null;
    }

}

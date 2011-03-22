/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.core.cache;

/**
 * Abstract class to be extended to provide new cache implementation
 *
 * @since 5.9.6
 */
public abstract class AbstractCache implements Cache {

    protected final String name;

    protected final int ttl;
    
    protected AbstractCache(CacheDescriptor desc) {
       name = desc.name;
       ttl = desc.ttl;
    }

    @Override
    public String getName() {
        return name;
    }


}

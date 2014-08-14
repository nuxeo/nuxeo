/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.core.cache;

import org.nuxeo.runtime.services.event.Event;

/**
 * Default in memory implementation for cache management based on guava
 * 
 * @author Maxime Hilaire
 * @since 5.9.6
 */
public class CacheManagerImpl extends AbstractCacheManager {

    @Override
    public boolean aboutToHandleEvent(Event arg0) {
        // TODO Auto-generated method stub
        // return false;
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleEvent(Event arg0) {
        // TODO Auto-generated method stub
        // 
        throw new UnsupportedOperationException();
    }


}

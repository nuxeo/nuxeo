/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @param <K>
 * @param <V>
 *
 * @author tiry
 */
public class LRUCachingMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    private final int maxCachedItems;

    public LRUCachingMap(int maxCachedItems) {
        super(maxCachedItems, 1.0f, true);
        this.maxCachedItems = maxCachedItems;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCachedItems;
    }

}

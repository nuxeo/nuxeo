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


/**
 * @author Maxime Hilaire
 *
 * @since 5.9.6
 */
public interface CacheManager {

    public static final String CORECACHEMANAGER_TOPIC = "corecachemanager";

    public String getName();

    public void setName(String name);

    public long getMaxSize();

    public void setMaxSize(long maxSize);

    public long getTtl();

    public void setTtl(long ttl);

    public long getConcurrencyLevel();

    public void setConcurrencyLevel(long concurrencyLevel);

}

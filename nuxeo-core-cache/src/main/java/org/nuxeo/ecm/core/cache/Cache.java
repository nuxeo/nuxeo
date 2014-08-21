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

import java.io.Serializable;

import org.nuxeo.runtime.services.event.EventListener;



/**
 * The nuxeo cache interface that define generic methods to use cache technologies 
 *
 * @since 5.9.6
 */
public interface Cache extends EventListener{

    public static final String CACHE_TOPIC = "cache-topic";

    public String getName();


    public Serializable get(String key);
    public void invalidate(String key);
    public void invalidateAll();

    public void put(String key, Serializable value);


}

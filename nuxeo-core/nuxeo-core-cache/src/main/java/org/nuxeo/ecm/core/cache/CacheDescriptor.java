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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Descriptor of cache contrib
 *
 * @since 5.9.6
 */
@XObject("cache")
public class CacheDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@remove")
    public boolean remove = false;

    @XNode("@class")
    protected Class<? extends Cache> implClass = InMemoryCacheImpl.class;

    @XNode("ttl")
    protected int ttl = 1;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> options = new HashMap<String, String>();

    protected CacheAttributesChecker cacheChecker;

    public CacheDescriptor() {
        super();
    }

    protected CacheDescriptor(String name, Class<? extends Cache> implClass,
            Integer ttl, Map<String, String> options) {
        this.name = name;
        this.implClass = implClass;
        this.ttl = ttl;
        this.options.putAll(options);
    }

    @Override
    public CacheDescriptor clone() {
        return new CacheDescriptor(name, implClass, ttl, options);
    }

    public Class<?> getImplClass() {
        return implClass;
    }

    public void setImplClass(Class<Cache> implClass) {
        this.implClass = implClass;
    }

    @Override
    public String toString() {
        return name + ": " + implClass + ": " + ttl + ": " + options;
    }

    public void start() {
        try {
            cacheChecker = new CacheAttributesChecker(this);
            cacheChecker.setCache(implClass.getConstructor(
                    CacheDescriptor.class).newInstance(this));
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new NuxeoException("Failed to instantiate class "
                    + implClass, e);
        }
    }

    public void stop() {
        if (cacheChecker == null) {
            return;
        }
        cacheChecker.cache = null;
        cacheChecker = null;
    }

}

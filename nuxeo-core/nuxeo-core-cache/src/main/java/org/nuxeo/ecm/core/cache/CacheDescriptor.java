/*******************************************************************************
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 ******************************************************************************/
package org.nuxeo.ecm.core.cache;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

@XObject("cache")
public class CacheDescriptor {

    @XContext()
    Context context;

    @XNode()
    Element document;

    @XNode("@name")
    public String name;

    @XNode("@remove")
    boolean remove = false;

    @XNode("@type")
    String type;

    @XNode(value="ttl",context="nuxeo.cache.ttl")
    public int ttl;

    @XNode("@class")
    @Deprecated
    void injectFactory(Class<? extends Cache> clazz) {
        type = Framework.getService(CacheFactoryRegistry.class).getType(clazz);
    }

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    @Deprecated
    void injectOptions(Map<String, String> options) {
        for (Map.Entry<String, String> option : options.entrySet()) {
            injectOption(option.getKey(), option.getValue());
        }
    }

    protected void injectOption(String name, String option) {

    }

    public String getName() {
        return name;
    }

    transient Cache cache;

    @Override
    public String toString() {
        return name + ":type=" + (type != null ? type : "default") + ",ttl=" + ttl;
    }

}

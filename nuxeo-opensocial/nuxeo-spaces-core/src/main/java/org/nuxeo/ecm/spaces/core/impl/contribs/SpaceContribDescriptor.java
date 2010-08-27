/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.core.impl.contribs;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.spaces.api.SpaceProvider;

@XObject("spaceContrib")
public class SpaceContribDescriptor implements
        Comparable<SpaceContribDescriptor> {

    @XNode("@name")
    private String name;

    @XNode("@remove")
    private boolean remove;

    @XNode("class")
    private Class<? extends SpaceProvider> klass;

    @XNode("order")
    private int order;

    @XNode("restrictToUniverse")
    private String pattern;

    @XNodeMap(value = "param", key = "@key", type = HashMap.class, componentType = String.class)
    protected Map<String, String> params;

    private SpaceProvider provider;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public int getOrder() {
        return order;
    }

    public String getPattern() {
        return pattern;
    }

    public SpaceProvider getProvider() throws InstantiationException,
            IllegalAccessException {
        if (provider == null) {
            provider = klass.newInstance();
            try {
                provider.initialize(params);
            } catch (Exception e) {
                throw new InstantiationException(
                        "Unable to initialize provider " + name);
            }
        }
        return provider;
    }

    public int compareTo(SpaceContribDescriptor o) {
        return this.getOrder() - o.getOrder();
    }

    public boolean matches(String universName) {
        if (pattern == null || pattern.equals("*")) {
            return true;
        }
        if (Pattern.matches(pattern, universName)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
      return "SpaceContribDescriptor [klass=" + klass + ", name=" + name
          + ", pattern=" + pattern + "]";
    }

}

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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.spaces.core.impl.UniversProvider;

@XObject("universContrib")
public class UniversContribDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@remove")
    private boolean remove;

    @XNode("class")
    private Class<? extends UniversProvider> klass;

    @XNodeMap(value = "param", key = "@key", type = HashMap.class, componentType = String.class)
    protected Map<String, String> params;

    private UniversProvider provider;

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

    public UniversProvider getProvider() throws InstantiationException,
            IllegalAccessException {
        if (provider == null) {
            provider = klass.newInstance();
            try {
                provider.initialize(params);
            } catch (Exception e) {
                throw new InstantiationException();
            }
        }
        return provider;
    }

}

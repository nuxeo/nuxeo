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

package org.nuxeo.ecm.spaces.impl;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.spaces.api.SpaceProvider;

@XObject("spaceProvider")
public class SpaceProviderDescriptor {

    @XNode("@name")
    String name;

    @XNode("@enabled")
    boolean enabled = true;

    @XNode("class")
    Class<? extends SpaceProvider> klass;

    @XNodeMap(value = "param", key = "@key", type = HashMap.class, componentType = String.class)
    Map<String, String> params;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Create the {@code SpaceProvider} instance and initialize it with this
     * {@code SpaceProviderDescriptor} attributes.
     *
     * @return a {@code SpaceProvider} instance.
     */
    public SpaceProvider getSpaceProvider() throws ClientException {
        try {
            SpaceProvider provider = klass.newInstance();
            provider.initialize(name, params);
            return provider;
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SpaceProviderDescriptor [klass=" + klass + ", name=" + name
                + "]";
    }

}

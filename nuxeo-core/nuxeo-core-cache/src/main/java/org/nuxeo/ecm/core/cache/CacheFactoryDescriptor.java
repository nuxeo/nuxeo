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

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

@XObject("factory")
public class CacheFactoryDescriptor {

    @XContext()
    Context context;

    @XNode("@name")
    String name;

    @XNode("@default")
    boolean isDefault;


    CacheFactory factory;

    @XNode("@type")
    public void setType(Class<? extends CacheFactory> type) {
        try {
            factory = type.newInstance();
        } catch (InstantiationException | IllegalAccessException cause) {
            throw new NuxeoException("Cannot create cache factory " + name, cause);
        }
    }

}

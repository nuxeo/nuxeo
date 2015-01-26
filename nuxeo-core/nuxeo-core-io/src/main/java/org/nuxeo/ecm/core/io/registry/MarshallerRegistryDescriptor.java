/*
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
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.registry;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * POJO used to handle "register" {@link MarshallerRegistry} extension point.
 *
 * @since 7.2
 */
@XObject("register")
public class MarshallerRegistryDescriptor {

    @XNode("@class")
    private Class<?> clazz;

    @XNode("@enable")
    private boolean enable;

    public MarshallerRegistryDescriptor() {
    }

    public MarshallerRegistryDescriptor(Class<?> clazz, boolean enable) {
        super();
        this.clazz = clazz;
        this.enable = enable;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public String toString() {
        return clazz.getName() + ":" + Boolean.toString(enable);
    }

}

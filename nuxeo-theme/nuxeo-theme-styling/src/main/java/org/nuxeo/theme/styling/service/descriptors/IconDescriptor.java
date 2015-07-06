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
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service.descriptors;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for icon elements.
 *
 * @since 7.4
 */
@XObject("icon")
public class IconDescriptor {

    @XNode("@name")
    protected String name;

    protected String value;

    @XNode("@sizes")
    protected String sizes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    @XContent
    public void setValue(String value) {
        if (value != null) {
            this.value = value.trim();
        } else {
            this.value = value;
        }
    }

    public String getSizes() {
        return sizes;
    }

    public void setSizes(String sizes) {
        this.sizes = sizes;
    }

    public IconDescriptor clone() {
        IconDescriptor clone = new IconDescriptor();
        clone.name = name;
        clone.value = value;
        clone.sizes = sizes;
        return clone;
    }

}

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
 *      Andre Justo
 */
package org.nuxeo.ecm.platform.ui.web.runtime;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 7.4
 */
@XObject("parameter")
public class JSFConfigurationDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@value")
    private String value;

    public JSFConfigurationDescriptor() {}

    public JSFConfigurationDescriptor(JSFConfigurationDescriptor other) {
        name = other.name;
        value = other.value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public JSFConfigurationDescriptor clone() {
        JSFConfigurationDescriptor clone = new JSFConfigurationDescriptor();
        doClone(clone);
        return clone;
    }

    protected void doClone(JSFConfigurationDescriptor clone) {
        clone.name = name;
        clone.value = value;
    }

    public void merge(JSFConfigurationDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.value != null) {
            value = other.value;
        }
    }
}

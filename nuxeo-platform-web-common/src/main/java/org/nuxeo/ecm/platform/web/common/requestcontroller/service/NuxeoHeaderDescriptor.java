/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Martins
 */

package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 * @since 6.0
 */
@XObject(value = "header")
public class NuxeoHeaderDescriptor implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled = true;

    protected String value;

    public String getValue() {
        return value;
    }

    @XContent
    public void setValue(String value) {
        if (value != null) {
            this.value = value.trim();
        }
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public NuxeoHeaderDescriptor clone() throws CloneNotSupportedException {
        NuxeoHeaderDescriptor d = new NuxeoHeaderDescriptor();
        d.name = name;
        d.enabled = enabled;
        d.value = value;
        return d;
    }

    public void merge(NuxeoHeaderDescriptor source) {
        enabled = source.enabled;
        if (source.value != null) {
            value = source.value;
        }
    }

}

/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.dam.core.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@XObject("inheritedProperties")
public class InheritedPropertiesDescriptor {

    @XNode("@schema")
    protected String schema;

    @XNode("@allProperties")
    protected boolean allProperties = false;

    @XNodeList(value = "property", type = ArrayList.class, componentType = String.class)
    protected List<String> properties;

    public String getSchema() {
        return schema;
    }

    public boolean allProperties() {
        return allProperties;
    }

    public List<String> getProperties() {
        return properties;
    }

}

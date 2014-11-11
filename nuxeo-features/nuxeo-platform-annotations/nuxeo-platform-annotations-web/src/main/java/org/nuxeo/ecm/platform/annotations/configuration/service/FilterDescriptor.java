/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     qlamerand
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.configuration.service;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Quentin Lamerand</a>
 *
 */
@XObject("filter")
public class FilterDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@order")
    private int order;

    @XNode("@icon")
    private String icon;

    @XNode("type")
    private String type;

    @XNode("author")
    private String author;

    @XNodeMap(value = "field", key = "@name", type = HashMap.class, componentType = String.class)
    private Map<String, String> fields;

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public String getIcon() {
        return icon;
    }

    public String getType() {
        return type;
    }

    public String getAuthor() {
        return author;
    }

    public Map<String, String> getFields() {
        return fields;
    }

}

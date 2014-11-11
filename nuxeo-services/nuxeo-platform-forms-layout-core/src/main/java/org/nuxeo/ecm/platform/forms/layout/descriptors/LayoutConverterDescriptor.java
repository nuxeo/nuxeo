/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.5
 */
@XObject("layoutConverter")
public class LayoutConverterDescriptor implements Serializable,
        Comparable<LayoutConverterDescriptor> {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    String name;

    @XNode("@order")
    int order = 0;

    @XNode("converter-class")
    String converterClassName;

    @XNodeList(value = "categories/category", type = String[].class, componentType = String.class)
    String[] categories = new String[0];

    public String getName() {
        return name;
    }

    public String getConverterClassName() {
        return converterClassName;
    }

    public String[] getCategories() {
        return categories;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public int compareTo(LayoutConverterDescriptor otherConverter) {
        int cmp = order - otherConverter.order;
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = name.compareTo(otherConverter.name);
        }
        return cmp;
    }

}

/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
@XObject("widgetConverter")
public class WidgetConverterDescriptor implements Serializable, Comparable<WidgetConverterDescriptor> {

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
    public int compareTo(WidgetConverterDescriptor otherConverter) {
        int cmp = order - otherConverter.order;
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = name.compareTo(otherConverter.name);
        }
        return cmp;
    }
}

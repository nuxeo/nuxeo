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

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetReferenceImpl;

/**
 * Descriptor for a widget reference with a row.
 * <p>
 * Makes it possible to reference a category on widget.
 *
 * @since 5.5
 */
@XObject("widget")
public class WidgetReferenceDescriptor {

    @XNode("@category")
    String category;

    @XContent
    String name;

    public String getCategory() {
        return category;
    }

    public String getName() {
        if (name != null) {
            String result = name.trim();
            result = result.replace("\n", "");
            return result;
        }
        return name;
    }

    public WidgetReference getWidgetReference() {
        return new WidgetReferenceImpl(category, getName());
    }

}

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

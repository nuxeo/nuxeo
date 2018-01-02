/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.theme.styling.service.descriptors;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for icon elements.
 *
 * @since 7.4
 */
@XObject("icon")
public class IconDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @Override
    public IconDescriptor clone() {
        IconDescriptor clone = new IconDescriptor();
        clone.name = name;
        clone.value = value;
        clone.sizes = sizes;
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IconDescriptor)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        IconDescriptor b = (IconDescriptor) obj;
        return new EqualsBuilder().append(name, b.name).append(value, b.value).append(sizes, b.sizes).isEquals();
    }

}

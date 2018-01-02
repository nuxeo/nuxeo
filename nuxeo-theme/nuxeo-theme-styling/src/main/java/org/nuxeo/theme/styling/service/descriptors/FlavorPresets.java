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
package org.nuxeo.theme.styling.service.descriptors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.5
 */
@XObject("presets")
public class FlavorPresets {

    @XNode("@category")
    String category;

    @XNode("@src")
    String src;

    /**
     * Resolved source content
     */
    String content;

    public String getCategory() {
        return category;
    }

    public String getSrc() {
        return src;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public FlavorPresets clone() {
        FlavorPresets clone = new FlavorPresets();
        clone.setSrc(src);
        clone.setCategory(category);
        clone.setContent(content);
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FlavorPresets)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        FlavorPresets f = (FlavorPresets) obj;
        // do not take content into account for overrides
        return new EqualsBuilder().append(category, f.category).append(src, f.src).isEquals();
    }

}

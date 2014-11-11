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
package org.nuxeo.theme.styling.service.descriptors;

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

}

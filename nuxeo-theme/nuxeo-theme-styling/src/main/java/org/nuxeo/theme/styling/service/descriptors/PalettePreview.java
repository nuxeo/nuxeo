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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.5
 */
@XObject("palettePreview")
public class PalettePreview {

    @XNodeList(value = "colors/color", type = ArrayList.class, componentType = String.class)
    List<String> colors;

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    @Override
    public PalettePreview clone() {
        PalettePreview clone = new PalettePreview();
        if (colors != null) {
            List<String> newColors = new ArrayList<>();
            newColors.addAll(colors);
            clone.setColors(newColors);
        }
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PalettePreview)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        PalettePreview p = (PalettePreview) obj;
        return new EqualsBuilder().append(colors, p.colors).isEquals();
    }

}

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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.4.3
 */
// TODO: use one file for all categories with a custom parser
@XObject("flavor")
public class Flavor {

    @XNode("@name")
    String name;

    @XNode("presetsList@append")
    boolean appendPresets;

    @XNodeList(value = "presetsList/presets", type = ArrayList.class, componentType = FlavorPresets.class)
    List<FlavorPresets> presets;

    public String getName() {
        return name;
    }

    public boolean getAppendPresets() {
        return appendPresets;
    }

    public List<FlavorPresets> getPresets() {
        return presets;
    }

    public void setPresets(List<FlavorPresets> presets) {
        this.presets = presets;
    }

}

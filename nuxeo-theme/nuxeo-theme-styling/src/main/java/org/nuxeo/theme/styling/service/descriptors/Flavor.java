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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * A flavor represents the set of information that can be used to switch the
 * theme styling on a given page.
 * <p>
 * It holds presets that can be referenced in CSS files, as well as logo
 * information. It can extend another flavor, in case it will its logo and
 * presets. The name and label are not inherited.
 * <p>
 * At registration, presets and log information are merged of a previous
 * contribution with the same name already held that kind of information. When
 * emptying the list of presets.
 *
 * @since 5.5
 */
@XObject("flavor")
public class Flavor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    String name;

    // TODO: add palette preview

    @XNode("label")
    String label;

    @XNode("@extends")
    String extendsFlavor;

    @XNode("logo")
    Logo logo;

    @XNode("palettePreview")
    PalettePreview palettePreview;

    @XNode("presetsList@append")
    boolean appendPresets;

    @XNodeList(value = "presetsList/presets", type = ArrayList.class, componentType = FlavorPresets.class)
    List<FlavorPresets> presets;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getExtendsFlavor() {
        return extendsFlavor;
    }

    public void setExtendsFlavor(String extendsFlavor) {
        this.extendsFlavor = extendsFlavor;
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

    public Logo getLogo() {
        return logo;
    }

    public void setLogo(Logo logo) {
        this.logo = logo;
    }

    public void setAppendPresets(boolean appendPresets) {
        this.appendPresets = appendPresets;
    }

    public PalettePreview getPalettePreview() {
        return palettePreview;
    }

    public void setPalettePreview(PalettePreview palettePreview) {
        this.palettePreview = palettePreview;
    }

    @Override
    public Flavor clone() {
        Flavor clone = new Flavor();
        clone.setName(getName());
        clone.setLabel(getLabel());
        Logo logo = getLogo();
        if (logo != null) {
            clone.setLogo(logo.clone());
        }
        PalettePreview pp = getPalettePreview();
        if (pp != null) {
            clone.setPalettePreview(pp.clone());
        }
        clone.setExtendsFlavor(getExtendsFlavor());
        clone.setAppendPresets(getAppendPresets());
        List<FlavorPresets> presets = getPresets();
        if (presets != null) {
            List<FlavorPresets> newPresets = new ArrayList<FlavorPresets>();
            for (FlavorPresets item : presets) {
                newPresets.add(item.clone());
            }
            clone.setPresets(newPresets);
        }
        return clone;
    }

}

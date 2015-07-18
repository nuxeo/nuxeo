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
 * A flavor represents the set of information that can be used to switch the theme styling on a given page.
 * <p>
 * It holds presets that can be referenced in CSS files, as well as logo information. It can extend another flavor, in
 * case it will its logo and presets. The name and label are not inherited.
 * <p>
 * At registration, presets and log information are merged of a previous contribution with the same name already held
 * that kind of information. When emptying the list of presets.
 *
 * @since 5.5
 */
@XObject("flavor")
public class FlavorDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    String name;

    // TODO: add palette preview

    @XNode("label")
    String label;

    @XNode("@extends")
    String extendsFlavor;

    @XNode("logo")
    LogoDescriptor logo;

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

    public LogoDescriptor getLogo() {
        return logo;
    }

    public void setLogo(LogoDescriptor logo) {
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

    public void merge(FlavorDescriptor src) {
        String newExtend = src.getExtendsFlavor();
        if (newExtend != null) {
            setExtendsFlavor(newExtend);
        }
        String newLabel = src.getLabel();
        if (newLabel != null) {
            setLabel(newLabel);
        }
        LogoDescriptor logo = src.getLogo();
        if (logo != null) {
            LogoDescriptor newLogo = getLogo();
            if (newLogo == null) {
                newLogo = logo.clone();
            } else {
                // merge logo info
                if (logo.getHeight() != null) {
                    newLogo.setHeight(logo.getHeight());
                }
                if (logo.getWidth() != null) {
                    newLogo.setWidth(logo.getWidth());
                }
                if (logo.getTitle() != null) {
                    newLogo.setTitle(logo.getTitle());
                }
                if (logo.getPath() != null) {
                    newLogo.setPath(logo.getPath());
                }
            }
            setLogo(newLogo);
        }
        PalettePreview pp = src.getPalettePreview();
        if (pp != null) {
            setPalettePreview(pp);
        }

        List<FlavorPresets> newPresets = src.getPresets();
        if (newPresets != null) {
            List<FlavorPresets> merged = new ArrayList<FlavorPresets>();
            merged.addAll(newPresets);
            boolean keepOld = src.getAppendPresets() || (newPresets.isEmpty() && !src.getAppendPresets());
            if (keepOld) {
                // add back old contributions
                List<FlavorPresets> oldPresets = getPresets();
                if (oldPresets != null) {
                    merged.addAll(0, oldPresets);
                }
            }
            setPresets(merged);
        }
    }

    @Override
    public FlavorDescriptor clone() {
        FlavorDescriptor clone = new FlavorDescriptor();
        clone.setName(getName());
        clone.setLabel(getLabel());
        LogoDescriptor logo = getLogo();
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

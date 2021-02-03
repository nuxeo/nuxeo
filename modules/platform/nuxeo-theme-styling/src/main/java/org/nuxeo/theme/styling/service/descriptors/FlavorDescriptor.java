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
import java.util.Collections;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

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
@XRegistry
public class FlavorDescriptor {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("label")
    protected String label;

    @XNode("@extends")
    protected String extendsFlavor;

    @XNode("logo")
    protected LogoDescriptor logo;

    @XNode("palettePreview")
    protected PalettePreview palettePreview;

    /**
     * @since 7.4
     */
    @XNodeList(value = "sass/import", type = ArrayList.class, componentType = SassImport.class)
    @XMerge(value = XMerge.MERGE, fallback = "sass@append")
    protected List<SassImport> sassImports = new ArrayList<>();

    @XNodeList(value = "presetsList/presets", type = ArrayList.class, componentType = FlavorPresets.class)
    @XMerge(value = XMerge.MERGE, fallback = "presetsList@append")
    protected List<FlavorPresets> presets = new ArrayList<>();

    /**
     * @since 7.4
     */
    @XNodeList(value = "links/icon", type = ArrayList.class, componentType = IconDescriptor.class)
    protected List<IconDescriptor> favicons = new ArrayList<>();

    // needed by xmap
    public FlavorDescriptor() {
    }

    // needed by service API
    public FlavorDescriptor(String name, String label, String extendsFlavor, LogoDescriptor logo,
            PalettePreview palettePreview, List<SassImport> sassImports, List<FlavorPresets> presets,
            List<IconDescriptor> favicons) {
        this.name = name;
        this.label = label;
        this.extendsFlavor = extendsFlavor;
        this.logo = logo;
        this.palettePreview = palettePreview;
        if (sassImports != null) {
            this.sassImports.addAll(sassImports);
        }
        if (presets != null) {
            this.presets = presets;
        }
        if (favicons != null) {
            this.favicons = favicons;
        }
    }

    public String getExtendsFlavor() {
        return extendsFlavor;
    }

    /**
     * @since 7.4
     */
    public List<IconDescriptor> getFavicons() {
        return favicons;
    }

    public String getLabel() {
        return label;
    }

    public LogoDescriptor getLogo() {
        return logo;
    }

    public String getName() {
        return name;
    }

    public PalettePreview getPalettePreview() {
        return palettePreview;
    }

    public List<FlavorPresets> getPresets() {
        return Collections.unmodifiableList(presets);
    }

    /**
     * @since 7.4
     */
    public List<SassImport> getSassImports() {
        return Collections.unmodifiableList(sassImports);
    }

}

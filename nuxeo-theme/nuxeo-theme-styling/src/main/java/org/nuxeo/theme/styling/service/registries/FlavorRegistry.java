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
package org.nuxeo.theme.styling.service.registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;
import org.nuxeo.theme.styling.service.descriptors.Flavor;
import org.nuxeo.theme.styling.service.descriptors.FlavorPresets;
import org.nuxeo.theme.styling.service.descriptors.Logo;
import org.nuxeo.theme.styling.service.descriptors.PalettePreview;

/**
 * Registry for theme flavors, handling merge of registered {@link Flavor}
 * elements.
 *
 * @since 5.5
 */
public class FlavorRegistry extends ContributionFragmentRegistry<Flavor> {

    protected Map<String, Flavor> themePageFlavors = new HashMap<String, Flavor>();

    @Override
    public String getContributionId(Flavor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, Flavor contrib,
            Flavor newOrigContrib) {
        themePageFlavors.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, Flavor origContrib) {
        themePageFlavors.remove(id);
    }

    @Override
    public Flavor clone(Flavor orig) {
        if (orig == null) {
            return null;
        }
        return orig.clone();
    }

    @Override
    public void merge(Flavor src, Flavor dst) {
        String newExtend = src.getExtendsFlavor();
        if (newExtend != null) {
            dst.setExtendsFlavor(newExtend);
        }
        String newLabel = src.getLabel();
        if (newLabel != null) {
            dst.setLabel(newLabel);
        }
        Logo logo = src.getLogo();
        if (logo != null) {
            Logo newLogo = dst.getLogo();
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
            dst.setLogo(newLogo);
        }
        PalettePreview pp = src.getPalettePreview();
        if (pp != null) {
            dst.setPalettePreview(pp);
        }

        List<FlavorPresets> newPresets = src.getPresets();
        if (newPresets != null) {
            List<FlavorPresets> merged = new ArrayList<FlavorPresets>();
            merged.addAll(newPresets);
            boolean keepOld = src.getAppendPresets()
                    || (newPresets.isEmpty() && !src.getAppendPresets());
            if (keepOld) {
                // add back old contributions
                List<FlavorPresets> oldPresets = dst.getPresets();
                if (oldPresets != null) {
                    merged.addAll(0, oldPresets);
                }
            }
            dst.setPresets(merged);
        }
    }

    public Flavor getFlavor(String id) {
        return themePageFlavors.get(id);
    }

    public List<Flavor> getFlavorsExtending(String flavor) {
        List<Flavor> res = new ArrayList<Flavor>();
        for (Flavor f : themePageFlavors.values()) {
            if (f != null) {
                String extendsFlavor = f.getExtendsFlavor();
                if (!StringUtils.isBlank(extendsFlavor)
                        && extendsFlavor.equals(flavor)) {
                    res.add(f);
                }
            }
        }
        return res;
    }

}

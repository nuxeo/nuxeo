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
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.core.ResourceBundleDescriptor;

/**
 * Descriptor to associate resources and flavors to a page.
 *
 * @since 7.4
 */
@XObject("page")
public class Page {

    public static final String RESOURCE_BUNDLE_PREFIX = "pageResourceBundle_";

    @XNode("@name")
    String name;

    /**
     * @since 7.4
     */
    @XNode("@charset")
    String charset;

    /**
     * @since 7.4
     */
    @XNodeList(value = "links/icon", type = ArrayList.class, componentType = IconDescriptor.class)
    List<IconDescriptor> favicons;

    @XNode("defaultFlavor")
    String defaultFlavor;

    /**
     * @deprecated since 7.4: use resources instead
     */
    @Deprecated
    @XNode("styles@append")
    boolean appendStyles;

    /**
     * @deprecated since 7.4: use resources instead
     */
    @Deprecated
    @XNodeList(value = "styles/style", type = ArrayList.class, componentType = String.class)
    List<String> styles;

    @XNode("flavors@append")
    boolean appendFlavors;

    @XNodeList(value = "flavors/flavor", type = ArrayList.class, componentType = String.class)
    List<String> flavors;

    @XNode("resources@append")
    boolean appendResources;

    @XNodeList(value = "resources/resource", type = ArrayList.class, componentType = String.class)
    List<String> resources;

    /**
     * @since 7.4
     */
    @XNodeList(value = "resources/bundle", type = ArrayList.class, componentType = String.class)
    List<String> bundles;

    public String getName() {
        return name;
    }

    public String getDefaultFlavor() {
        return defaultFlavor;
    }

    public void setDefaultFlavor(String defaultFlavor) {
        this.defaultFlavor = defaultFlavor;
    }

    /**
     * @deprecated since 7.4: use resources instead
     */
    public boolean getAppendStyles() {
        return appendStyles;
    }

    /**
     * @deprecated since 7.4: use resources instead
     */
    public List<String> getStyles() {
        return styles;
    }

    public boolean getAppendFlavors() {
        return appendFlavors;
    }

    public List<String> getFlavors() {
        return flavors;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStyles(List<String> styles) {
        this.styles = styles;
    }

    public void setFlavors(List<String> flavors) {
        this.flavors = flavors;
    }

    public boolean getAppendResources() {
        return appendResources;
    }

    public boolean hasResources() {
        return !getResources().isEmpty();
    }

    public List<String> getResources() {
        List<String> res = new ArrayList<String>();
        // BBB
        if (styles != null) {
            for (String style : styles) {
                if (style == null) {
                    continue;
                }
                if (style.endsWith(ResourceType.css.name())) {
                    res.add(style);
                } else {
                    res.add(style + "." + ResourceType.css.name());
                }
            }
        }
        if (resources != null) {
            res.addAll(resources);
        }
        return res;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public String getComputedResourceBundleName() {
        if ("*".equals(getName())) {
            return RESOURCE_BUNDLE_PREFIX + "*";
        }
        return RESOURCE_BUNDLE_PREFIX + getName().replaceAll("[^a-zA-Z]+", "_");
    }

    public ResourceBundle getComputedResourceBundle() {
        if (hasResources()) {
            ResourceBundleDescriptor bundle = new ResourceBundleDescriptor();
            bundle.setName(getComputedResourceBundleName());
            bundle.setResources(getResources());
            bundle.setAppend(getAppendResources());
            return bundle;
        }
        return null;
    }

    /**
     * @since 7.4
     */
    public List<String> getResourceBundles() {
        List<String> all = new ArrayList<String>();
        if (bundles != null) {
            all.addAll(bundles);
        }
        if (hasResources()) {
            all.add(getComputedResourceBundleName());
        }
        return all;
    }

    /**
     * @since 7.4
     */
    public void setResourceBundles(List<String> bundles) {
        this.bundles = bundles;
    }

    public void setAppendStyles(boolean appendStyles) {
        this.appendStyles = appendStyles;
    }

    public void setAppendFlavors(boolean appendFlavors) {
        this.appendFlavors = appendFlavors;
    }

    public void setAppendResources(boolean appendResources) {
        this.appendResources = appendResources;
    }

    /**
     * @since 7.4
     */
    public String getCharset() {
        return charset;
    }

    /**
     * @since 7.4
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * @since 7.4
     */
    public List<IconDescriptor> getFavicons() {
        return favicons;
    }

    /**
     * @since 7.4
     */
    public void setFavicons(List<IconDescriptor> favicons) {
        this.favicons = favicons;
    }

    public void merge(Page src) {
        String newFlavor = src.getDefaultFlavor();
        if (newFlavor != null) {
            setDefaultFlavor(newFlavor);
        }

        String newCharset = src.getCharset();
        if (newCharset != null) {
            setCharset(newCharset);
        }

        List<IconDescriptor> newFavicons = src.getFavicons();
        if (newFavicons != null && !newFavicons.isEmpty()) {
            setFavicons(newFavicons);
        }

        List<String> newStyles = src.getStyles();
        if (newStyles != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newStyles);
            boolean keepOld = src.getAppendStyles() || (newStyles.isEmpty() && !src.getAppendStyles());
            if (keepOld) {
                // add back old contributions
                List<String> oldStyles = getStyles();
                if (oldStyles != null) {
                    merged.addAll(0, oldStyles);
                }
            }
            setStyles(merged);
        }

        List<String> newFlavors = src.getFlavors();
        if (newFlavors != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newFlavors);
            boolean keepOld = src.getAppendFlavors() || (newFlavors.isEmpty() && !src.getAppendFlavors());
            if (keepOld) {
                // add back old contributions
                List<String> oldFlavors = getFlavors();
                if (oldFlavors != null) {
                    merged.addAll(0, oldFlavors);
                }
            }
            setFlavors(merged);
        }

        List<String> newResources = src.resources;
        if (newResources != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newResources);
            boolean keepOld = src.getAppendResources() || (newResources.isEmpty() && !src.getAppendResources());
            if (keepOld) {
                // add back old contributions
                List<String> oldResources = resources;
                if (oldResources != null) {
                    merged.addAll(0, oldResources);
                }
            }
            setResources(merged);
        }

        List<String> newBundles = src.bundles;
        if (newBundles != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newBundles);
            boolean keepOld = src.getAppendResources() || (newBundles.isEmpty() && !src.getAppendResources());
            if (keepOld) {
                // add back old contributions
                List<String> oldBundles = bundles;
                if (oldBundles != null) {
                    merged.addAll(0, oldBundles);
                }
            }
            setResourceBundles(merged);
        }
    }

    @Override
    public Page clone() {
        Page clone = new Page();
        clone.setName(getName());
        clone.setCharset(getCharset());
        List<IconDescriptor> favicons = getFavicons();
        if (favicons != null) {
            List<IconDescriptor> icons = new ArrayList<IconDescriptor>();
            for (IconDescriptor icon : favicons) {
                icons.add(icon.clone());
            }
            clone.setFavicons(icons);
        }
        clone.setDefaultFlavor(getDefaultFlavor());
        clone.setAppendStyles(getAppendStyles());
        List<String> styles = getStyles();
        if (styles != null) {
            clone.setStyles(new ArrayList<String>(styles));
        }
        clone.setAppendFlavors(getAppendFlavors());
        List<String> flavors = getFlavors();
        if (flavors != null) {
            clone.setFlavors(new ArrayList<String>(flavors));
        }
        clone.setAppendResources(getAppendResources());
        if (resources != null) {
            clone.setResources(new ArrayList<String>(resources));
        }
        if (bundles != null) {
            clone.setResourceBundles(new ArrayList<String>(bundles));
        }
        return clone;
    }

}

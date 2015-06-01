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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.services.ThemeService;

/**
 * Descriptor to associate resources and flavors to a page.
 *
 * @since 7.3
 */
@XObject("page")
public class Page {

    @XNode("@name")
    String name;

    /**
     * @since 7.3
     */
    @XNode("@charset")
    String charset;

    /**
     * @since 7.3
     */
    @XNodeMap(value = "links/icon", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> favicons = new HashMap<String, String>();

    @XNode("defaultFlavor")
    String defaultFlavor;

    /**
     * @deprecated since 7.3: use resources instead
     */
    @Deprecated
    @XNode("styles@append")
    boolean appendStyles;

    /**
     * @deprecated since 7.3: use resources instead
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
     * @since 7.3
     */
    @XNodeList(value = "resources/bundle", type = ArrayList.class, componentType = String.class)
    List<String> bundles;

    /**
     * boolean handling the descriptor status: has it been already loaded to the {@link ThemeService}?
     */
    boolean loaded = false;

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
     * @deprecated since 7.3: use resources instead
     */
    public boolean getAppendStyles() {
        return appendStyles;
    }

    /**
     * @deprecated since 7.3: use resources instead
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

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    /**
     * @since 7.3
     */
    public List<String> getResourceBundles() {
        return bundles;
    }

    /**
     * @since 7.3
     */
    public void setResourceBundles(List<String> bundles) {
        this.bundles = bundles;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
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
     * @since 7.3
     */
    public String getCharset() {
        return charset;
    }

    /**
     * @since 7.3
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * @since 7.3
     */
    public Map<String, String> getFavicons() {
        return favicons;
    }

    /**
     * @since 7.3
     */
    public void setFavicons(Map<String, String> favicons) {
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

        Map<String, String> newFavicons = src.getFavicons();
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

        List<String> newResources = src.getResources();
        if (newResources != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newResources);
            boolean keepOld = src.getAppendResources() || (newResources.isEmpty() && !src.getAppendResources());
            if (keepOld) {
                // add back old contributions
                List<String> oldResources = getResources();
                if (oldResources != null) {
                    merged.addAll(0, oldResources);
                }
            }
            setResources(merged);
        }

        List<String> newBundles = src.getResourceBundles();
        if (newBundles != null) {
            List<String> merged = new ArrayList<String>();
            merged.addAll(newBundles);
            boolean keepOld = src.getAppendResources() || (newBundles.isEmpty() && !src.getAppendResources());
            if (keepOld) {
                // add back old contributions
                List<String> oldBundles = getResourceBundles();
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
        Map<String, String> favicons = new HashMap<String, String>();
        if (favicons != null) {
            clone.setFavicons(new HashMap<>(favicons));
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
        List<String> resources = getResources();
        if (resources != null) {
            clone.setResources(new ArrayList<String>(resources));
        }
        List<String> bundles = getResourceBundles();
        if (bundles != null) {
            clone.setResourceBundles(new ArrayList<String>(bundles));
        }
        clone.setLoaded(isLoaded());
        return clone;
    }

}

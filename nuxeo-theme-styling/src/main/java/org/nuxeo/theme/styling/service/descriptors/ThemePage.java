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
import org.nuxeo.theme.services.ThemeService;

/**
 * Descriptor to associate resources to a theme page
 *
 * @since 5.5
 */
@XObject("themePage")
public class ThemePage {

    @XNode("@name")
    String name;

    @XNode("defaultFlavor")
    String defaultFlavor;

    @XNode("styles@append")
    boolean appendStyles;

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
     * boolean handling the descriptor status: has it been already loaded to
     * the {@link ThemeService}?
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

    public boolean getAppendStyles() {
        return appendStyles;
    }

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

    public static String getPageName(String themePage) {
        if ("*".equals(themePage)) {
            return "*";
        }
        try {
            String[] nameEl = themePage.split("/");
            return nameEl[1];
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "Invalid theme page name '%s': cannot retrieve page name",
                    themePage));
        }
    }

    public static String getThemeName(String themePage) {
        if ("*".equals(themePage)) {
            return "*";
        }
        try {
            String[] nameEl = themePage.split("/");
            return nameEl[0];
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "Invalid theme page name '%s': cannot retrieve theme name",
                    themePage));
        }
    }

}

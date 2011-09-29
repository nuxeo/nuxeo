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
 * @since 5.4.3
 */
@XObject("themePage")
public class ThemePage {

    @XNode("@name")
    String name;

    @XNode("defaultFlavour")
    String defaultFlavour;

    @XNode("styles/append")
    boolean appendStyles;

    @XNodeList(value = "styles/style", type = ArrayList.class, componentType = String.class)
    List<String> styles;

    @XNode("flavours/append")
    boolean appendFlavours;

    @XNodeList(value = "flavours/flavour", type = ArrayList.class, componentType = String.class)
    List<String> flavours;

    /**
     * boolean handling the descriptor status: has it been already loaded to
     * the {@link ThemeService}?
     */
    boolean loaded = false;

    public String getName() {
        return name;
    }

    public String getThemeName() {
        try {
            String[] nameEl = name.split("/");
            return nameEl[0];
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "Invalid theme page name '%s': cannot retrieve theme name",
                    name));
        }
    }

    public String getDefaultFlavour() {
        return defaultFlavour;
    }

    public void setDefaultFlavour(String defaultFlavour) {
        this.defaultFlavour = defaultFlavour;
    }

    public boolean getAppendStyles() {
        return appendStyles;
    }

    public List<String> getStyles() {
        return styles;
    }

    public boolean getAppendFlavours() {
        return appendFlavours;
    }

    public List<String> getFlavours() {
        return flavours;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStyles(List<String> styles) {
        this.styles = styles;
    }

    public void setFlavours(List<String> flavours) {
        this.flavours = flavours;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

}

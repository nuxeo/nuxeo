/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.themes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("themeset")
public class ThemeSet implements Type {

    @XNode("@name")
    public String name;

    @XNodeMap(value = "theme", key = "@name", type = HashMap.class, componentType = ThemeSetEntry.class)
    public Map<String, ThemeSetEntry> themes;

    public ThemeSet() {
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.THEMESET;
    }

    public String getTypeName() {
        return name;
    }

    public String getName() {
        return name;
    }

    /*
     * Theme entries
     */
    public void setTheme(ThemeSetEntry theme) {
        themes.put(theme.getName(), theme);
    }

    public ThemeSetEntry getTheme(String themeName) {
        return themes.get(themeName);
    }

    public List<ThemeSetEntry> getThemes() {
        return new ArrayList<ThemeSetEntry>(themes.values());
    }

    /*
     * Features
     */
    public String getThemeForFeature(String feature) {
        for (ThemeSetEntry theme : getThemes()) {
            if (theme.getFeatures().contains(feature)) {
                return theme.getName();
            }
        }
        return null;
    }

    public void addFeatureToTheme(String themeName, String feature) {
        for (ThemeSetEntry theme : getThemes()) {
            if (theme.getName().equals(themeName)) {
                theme.addFeature(feature);
            } else {
                theme.removeFeature(feature);
            }
        }
    }

}

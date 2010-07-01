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

import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("theme")
public class ThemeSetEntry {

    @XNode("@name")
    public String name;

    @XNode("@features")
    public String features = "";

    public ThemeSetEntry() {
    }

    public ThemeSetEntry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getFeatures() {
        return Arrays.asList(features.split(","));
    }

    public void addFeature(String feature) {
        if (!getFeatures().contains(feature)) {
            features += features.concat(",").concat(feature);
        }
    }

    public void removeFeature(String feature) {
        List<String> featuresList = getFeatures();
        features = "";
        for (String f : featuresList) {
            if (!f.equals(feature)) {
                addFeature(f);
            }
        }
    }

}

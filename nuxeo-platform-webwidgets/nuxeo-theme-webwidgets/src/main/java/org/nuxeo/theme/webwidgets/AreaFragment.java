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

package org.nuxeo.theme.webwidgets;

import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.properties.FieldInfo;

public final class AreaFragment extends AbstractFragment {

    @FieldInfo(type = "string", label = "region name", description = "The name of the region.", required = true)
    public String name = "";

    @FieldInfo(type = "selection", label = "provider", source = "web widget providers", description = "The widget provider.", required = true)
    public String provider = "default";

    @FieldInfo(type = "selection", label = "decoration", source = "web widget decorations", description = "The panel and widget decoration.", required = true)
    public String decoration = "default";

    @FieldInfo(type = "integer", label = "maximum number of widgets", description = "The maximum number of widgets allowed in this area.")
    public Integer maxItems;

    @FieldInfo(type = "boolean", label = "Disallow duplicates", description = "Do not allow more than one widget of the same type in this area.")
    public Boolean disallowDuplicates;

    public AreaFragment() {
    }

    public AreaFragment(String name, String provider, String decoration,
            Integer maxItems, Boolean disallowDuplicates) {
        this.name = name;
        this.provider = provider;
        this.decoration = decoration;
        this.maxItems = maxItems;
        this.disallowDuplicates = disallowDuplicates;
    }

    @Override
    public Model getModel() {
        return new RegionModel(name, provider, decoration);
    }

    // Model
    public String getProviderName() {
        return provider;
    }

    public String getRegionName() {
        return name;
    }

    public String getDecoration() {
        return decoration;
    }

    // Extra options
    public Integer getMaxItems() {
        return maxItems;
    }

    public Boolean getDisallowDuplicates() {
        return disallowDuplicates;
    }
}

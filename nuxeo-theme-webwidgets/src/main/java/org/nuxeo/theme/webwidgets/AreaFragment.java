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
import org.nuxeo.theme.properties.FieldInfo;
import org.nuxeo.theme.models.Model;

public final class AreaFragment extends AbstractFragment {

    @FieldInfo(type = "string", label = "region name", description = "The name of the region.", required = true)
    public String name = "";

    @FieldInfo(type = "selection", label = "provider", source = "web widget providers", description = "The widget provider.", required = true)
    public String provider = "default";

    @FieldInfo(type = "selection", label = "decoration", source = "web widget decorations", description = "The panel and widget decoration.", required = true)
    public String decoration = "default";

    public AreaFragment() {
    }

    public AreaFragment(String name, String provider, String security,
            String decoration) {
        this.name = name;
        this.provider = provider;
        this.decoration = decoration;
    }

    @Override
    public Model getModel() {
        return new RegionModel(name, provider, decoration);
    }

}

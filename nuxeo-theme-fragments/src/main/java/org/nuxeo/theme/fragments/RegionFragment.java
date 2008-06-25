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

package org.nuxeo.theme.fragments;

import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.Region;
import org.nuxeo.theme.properties.FieldInfo;

public final class RegionFragment extends AbstractFragment {

    @FieldInfo(type = "string", label = "region name", description = "The name of the region from which content is inserted.")
    public String name = "";

    @FieldInfo(type = "text area", label = "default body", description = "The default HTML content to insert if the region is not filled.")
    public String defaultBody = "";

    @FieldInfo(type = "string", label = "default source", description = "The source of the HTML content to insert if the region is not filled.")
    public String defaultSrc = "";

    public RegionFragment() {
    }

    public RegionFragment(String name, String defaultBody, String defaultSrc) {
        this.name = name;
        this.defaultBody = defaultBody;
        this.defaultSrc = defaultSrc;
    }

    @Override
    public Model getModel() {
        return new Region(name, defaultBody, defaultSrc);
    }

}

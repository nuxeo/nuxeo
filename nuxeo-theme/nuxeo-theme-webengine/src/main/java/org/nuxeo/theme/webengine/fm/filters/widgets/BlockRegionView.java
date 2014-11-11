/*
 * (C) Copyright 2006-2008 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.webengine.fm.filters.widgets;

import org.nuxeo.theme.models.Region;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.views.AbstractView;

public final class BlockRegionView extends AbstractView {

    @Override
    public String render(final RenderingInfo info) {
        final Region region = (Region) info.getModel();
        final StringBuilder s = new StringBuilder();
        if (region.name != "") {
            s.append("<div class=\"nxthemesRegion\">");
            s.append("<@block name=\"").append(region.name).append("\">");
            if (!region.defaultSrc.equals("")) {
                s.append("<#include \"").append(region.defaultSrc).append(
                        "\" />");
            } else {
                s.append(region.defaultBody);
            }
            s.append("</@block>");
            s.append("</div>");
        } else {
            s.append("<div class=\"nxthemesRegionNotSet\">").append(
                    "No region name is set...").append("</div>");
        }
        return s.toString();
    }

}

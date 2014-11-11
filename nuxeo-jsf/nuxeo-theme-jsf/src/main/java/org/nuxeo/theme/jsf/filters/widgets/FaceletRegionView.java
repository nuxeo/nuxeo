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

package org.nuxeo.theme.jsf.filters.widgets;

import org.nuxeo.theme.models.Region;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.views.AbstractView;

public final class FaceletRegionView extends AbstractView {

    @Override
    public String render(final RenderingInfo info) {
        final Region region = (Region) info.getModel();
        final StringBuilder s = new StringBuilder();

        if ("".equals(region.name)) {
            s.append("<div class=\"nxthemesRegionNotSet\">").append(
                    "No facelet region name is set...").append("</div>");
        } else {
            s.append("<div xmlns:ui=\"http://java.sun.com/jsf/facelets\""
                    + " class=\"themeRegion\">");
            s.append("<ui:insert name=\"").append(region.name).append("\">");
            if ("".equals(region.defaultSrc)) {
                s.append(region.defaultBody);
            } else {
                s.append("<ui:include src=\"").append(region.defaultSrc).append(
                        "\" />");
            }
            s.append("</ui:insert>");
            s.append("</div>");
        }
        return s.toString();
    }

}

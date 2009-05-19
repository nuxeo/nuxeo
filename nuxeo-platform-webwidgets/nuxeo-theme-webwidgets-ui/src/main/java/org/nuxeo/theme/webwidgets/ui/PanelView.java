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

package org.nuxeo.theme.webwidgets.ui;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.theme.html.Utils;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.views.AbstractView;
import org.nuxeo.theme.webwidgets.Manager;
import org.nuxeo.theme.webwidgets.ProviderType;
import org.nuxeo.theme.webwidgets.DecorationType;
import org.nuxeo.theme.webwidgets.RegionModel;

public final class PanelView extends AbstractView {

    @Override
    public String render(final RenderingInfo info) {
        final RegionModel region = (RegionModel) info.getModel();
        final String regionName = region.name;
        final String providerName = region.provider;
        final String decorationName = region.decoration;
        final String engineName = info.getEngine().getName();
        final String viewMode = info.getViewMode();
        final int areaUid = info.getElement().getUid();
        final String displayedRegionName = regionName.equals("") ? "&lt;Please set the region name&gt;"
                : regionName;

        // Register resources
        final URL themeUrl = info.getThemeUrl();
        final ResourceManager resourceManager = org.nuxeo.theme.Manager.getResourceManager();
        DecorationType decorationType = Manager.getDecorationType(decorationName);
        for (String resource : decorationType.getResources()) {
            resourceManager.addResource(resource, themeUrl);
        }

        final StringBuilder s = new StringBuilder();

        /* Theme editor mode: manage web widget panels inside a theme */
        if (engineName.equals("page-editor")) {
            ProviderType providerType = Manager.getProviderType(providerName);
            String description = "";
            if (providerType != null) {
                description = providerType.getDescription();
            }
            s.append("<div class=\"nxthemesPageArea\">");
            s.append(String.format("<div class=\"title\">%s</div>",
                    displayedRegionName));
            s.append(String.format(
                    "<div class=\"body\">Web widget area. <p>Provider: <strong>%s</strong> (%s)</p></div>",
                    providerName, description));
            s.append("</div>");
        }

        /* Style editor preview */
        else if (engineName.equals("fragments-only")) {
            final String content = "&lt;PANEL AREA&gt;<div class=\"nxthemesWebWidget\">&lt;WEB WIDGET&gt;</div>";
            final String panelContent = Manager.addPanelDecoration(
                    decorationName, "*", displayedRegionName, content);
            s.append(String.format("<div>%s</div>", panelContent));
        }

        /* Web widgets edit mode */
        else if (viewMode.equals("web-widgets")) {
            final StringBuilder sb = new StringBuilder();
            if (!regionName.equals("")) {
                final String identifier = String.format("web widget panel %s",
                        info.getUid());
                final String model = String.format(
                        "{\"data\":{\"area\":\"%s\",\"provider\":\"%s\",\"decoration\":\"%s\",\"mode\":\"%s\"},\"id\":\"%s\"}",
                        areaUid, providerName, decorationName, viewMode, identifier);
                final String view = String.format(
                        "{\"controllers\":[\"web widget perspectives\",\"web widget inserter\",\"web widget mover\"],\"widget\":{\"type\":\"web widget panel\"},\"model\":\"%s\",\"perspectives\":[\"default\"],\"id\":\"%s\"}",
                        identifier, identifier);
                sb.append(String.format("<ins class=\"model\">%s</ins>", model));
                sb.append(String.format("<ins class=\"view\">%s</ins>", view));
            }
            s.append(Manager.addPanelDecoration(decorationName, viewMode,
                    displayedRegionName, sb.toString()));
        }

        /* Default rendering mode */
        else if (!regionName.equals("")) {
            final Map<String, Object> data = new HashMap<String, Object>();
            data.put("area", areaUid);
            data.put("mode", viewMode);
            final String content = String.format(
                    "<ins style=\"display: none\" class=\"nxthemesWebWidgetPanel\">%s</ins>",
                    Utils.toJson(data));
            s.append(String.format("<div>%s</div>", Manager.addPanelDecoration(
                    decorationName, viewMode, displayedRegionName, content)));
        }

        return s.toString();
    }
}

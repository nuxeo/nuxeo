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

package org.nuxeo.theme.editor.views;

import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.html.filters.layout.DefaultLayoutView;
import org.nuxeo.theme.rendering.RenderingInfo;

public class SectionLayout extends DefaultLayoutView {

    static final String[] SIZER_PROPERTIES = { "width", "margin-left",
            "margin-right" };

    @Override
    public String render(final RenderingInfo info) {
        final Format layout = info.getFormat();
        final StringBuilder sizerStyle = new StringBuilder();

        // Sizer style
        for (String property : SIZER_PROPERTIES) {
            final String value = layout.getProperty(property);
            if (value != null) {
                sizerStyle.append(String.format("%s: %s;", property, value));
            }
        }

        // Form values
        String width = layout.getProperty("width");
        if (width == null) {
            width = "";
        }

        final String sectionId = info.getElement().getUid().toString();

        final StringBuilder html = new StringBuilder();
        html.append("<div class=\"nxthemesSectionLayout\">");
        html.append("<form style=\"").append(sizerStyle.toString()).append(
                "\" class=\"nxthemesSizer\" action=\"\" onsubmit=\"return false\">").append(
                "<b class=\"left\"></b><b class=\"right\"></b>").append(
                "<input class=\"nxthemesInput\" type=\"text\" size=\"5\"").append(
                " value=\"").append(width).append("\" name=\"width\" />").append(
                "<input type=\"hidden\" name=\"id\" value=\"").append(
                layout.getUid().toString()).append("\" /></form>");
        html.append(super.render(info));

        html.append("<table class=\"nxthemesAlignSection\"><tr>");
        html.append(
                "<td><div class=\"nxthemesAlignSectionLeft\" title=\"Left\" sectionid=\"").append(
                sectionId).append("\"></div></td>");
        html.append(
                "<td><div class=\"nxthemesAlignSectionCenter\" title=\"Center\" sectionid=\"").append(
                sectionId).append("\" ></div></td>");
        html.append(
                "<td><div class=\"nxthemesAlignSectionRight\" title=\"Right\" sectionid=\"").append(
                sectionId).append("\" ></div></td>");
        html.append("</tr></table>");

        html.append(
                "<a href=\"javascript:void(0)\" class=\"nxthemesAddSection\" title=\"Add a section\"><div sectionid=\"").append(
                sectionId).append("\" > Add a section</div></a>");

        html.append("</div>");
        return html.toString();
    }
}

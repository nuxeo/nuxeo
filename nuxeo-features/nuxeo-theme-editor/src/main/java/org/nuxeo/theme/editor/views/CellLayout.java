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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.html.filters.layout.DefaultLayoutView;
import org.nuxeo.theme.rendering.RenderingInfo;

public class CellLayout extends DefaultLayoutView {

    static final Pattern contentPattern = Pattern.compile("<.*?>(.*)</td>",
            Pattern.DOTALL);

    @Override
    public String render(final RenderingInfo info) {
        final String markup = info.getMarkup();
        final Matcher contentMatcher = contentPattern.matcher(markup);

        if (!contentMatcher.find()) {
            return markup;
        }

        // write the final markup
        final Format layout = info.getFormat();
        final StringBuilder style = new StringBuilder();
        String width = layout.getProperty("width");
        if (width != null) {
            style.append("width:").append(width).append(';');
        } else {
            width = "";
        }
        String textAlign = layout.getProperty("text-align");
        if (textAlign != null) {
            style.append("text-align:").append(textAlign).append(';');
        }

        final String cellId = info.getElement().getUid().toString();

        final StringBuilder html = new StringBuilder();
        html.append(String.format(
                "<td class=\"nxthemesCellLayout nxthemesContainer\" style=\"%s\" id=\"e%s\">",
                style.toString(), cellId));

        html.append(
                "<form class=\"nxthemesSizer\" action=\"\" onsubmit=\"return false\">").append(
                "<b class=\"left\"></b><b class=\"right\"></b>").append(
                "<input class=\"nxthemesInput\" type=\"text\" size=\"5\"").append(
                " value=\"").append(width).append("\" name=\"width\" />").append(
                "<input type=\"hidden\" name=\"id\" value=\"").append(
                layout.getUid().toString()).append("\" /></form>");

        html.append(contentMatcher.group(1));

        html.append("</td>");
        return html.toString();
    }
}

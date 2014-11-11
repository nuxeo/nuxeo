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

import java.util.Properties;

import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.rendering.RenderingInfo;

public class AreaStyleToolbox {

    public static String render(final RenderingInfo info) {
        final Style style = (Style) ElementFormatter.getFormatFor(info.getElement(),
                "style");
        Properties properties = null;
        if (style != null) {
            String viewName = info.getFormat().getName();
            properties = style.getPropertiesFor(viewName, "");
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"toolbox\">");
        sb.append(renderIcon(properties, "background", "background",
                "area-style-background.png"));
        sb.append(renderIcon(properties, "border-left", "left border",
                "area-style-border-left.png"));
        sb.append(renderIcon(properties, "border-top", "top border",
                "area-style-border-top.png"));
        sb.append(renderIcon(properties, "border-bottom", "bottom border",
                "area-style-border-bottom.png"));
        sb.append(renderIcon(properties, "border-right", "right border",
                "area-style-border-right.png"));
        sb.append("</div>");
        return sb.toString();
    }

    private static String renderIcon(final Properties properties, final String name,
            final String title, final String image) {
        String className = "picker";
        if (properties != null && properties.getProperty(name) != null) {
            className = "picker selected";
        }
        return String.format(
                "<img class=\"%s\" name=\"%s\" width=\"16\" height=\"16\" title=\"%s\" src=\"/nuxeo/nxthemes/editor/img/%s\" />",
                className, name, title, image);
    }
}

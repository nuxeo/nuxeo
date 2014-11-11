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
        final Style style = (Style) ElementFormatter.getFormatFor(
                info.getElement(), "style");
        Properties properties = null;
        if (style != null) {
            String viewName = info.getFormat().getName();
            properties = style.getPropertiesFor(viewName, "");
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"toolbox\"><tr>");
        sb.append(renderIcon(properties, "background", "background",
                "areaStyleBackground"));
        sb.append(renderIcon(properties, "border-left", "left border",
                "areaStyleBorderLeft"));
        sb.append(renderIcon(properties, "border-top", "top border",
                "areaStyleBorderTop"));
        sb.append(renderIcon(properties, "border-bottom", "bottom border",
                "areaStyleBorderBottom"));
        sb.append(renderIcon(properties, "border-right", "right border",
                "areaStyleBorderRight"));
        sb.append("</tr></table>");
        return sb.toString();
    }

    private static String renderIcon(final Properties properties,
            final String name, final String title, final String iconClassName) {
        String className = "picker";
        if (properties != null && properties.getProperty(name) != null) {
            className = "picker selected";
        }
        return String.format(
                "<td><div class=\"%s %s\" name=\"%s\" title=\"%s\"></div></td>",
                className, iconClassName, name, title);
    }
}

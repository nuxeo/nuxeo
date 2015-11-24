/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.container.client.api.adapter.html;

import java.io.Serializable;

import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIComponentZoneImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIUnitImpl;

/**
 * @author Stéphane Fourrier
 */
public class YUILayoutHtmlAdapter implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String HEADER_ID = "hd";

    private static final String FOOTER_ID = "ft";

    private YUILayout layout;

    public YUILayoutHtmlAdapter(YUILayout layout) {
        this.layout = layout;
    }

    public String toHtml() {
        StringBuilder sb = new StringBuilder("<div id=\""
                + layout.getBodySize().getCSS() + "\" class=\""
                + layout.getSidebarStyle().getCSS() + "\">\n");

        if (layout.getHeader() != null) {
            sb.append("\t<div id=\"" + HEADER_ID + "\">\n");
            sb.append("\t\t<!-- Header here -->\n");
            sb.append("\t</div>\n");
        }

        sb.append("\t<div id=\"" + layout.getContent().getId() + "\">\n");
        sb.append("\t\t<div id=\"yui-main\">\n");
        sb.append("\t\t\t<div class=\"yui-b\">\n");

        for (YUIComponent component : layout.getContent().getComponents()) {
            sb.append(getComponentAsHtml(component));
        }

        sb.append("\t\t\t</div>\n");
        sb.append("\t\t</div>\n");

        if (!layout.getSidebarStyle().toString().equals("YUI_SB_NO_COLUMN")) {
            sb.append("\t\t<div class=\"yui-b\">\n");
            sb.append("\t\t\t<!-- Sidebar here -->\n");
            sb.append("\t\t</div>\n");
        }
        sb.append("\t</div>\n");

        if (layout.getFooter() != null) {
            sb.append("\t<div id=\"" + FOOTER_ID + "\">\n");
            sb.append("\t\t<!-- Footer here -->\n");
            sb.append("\t</div>\n");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private String getComponentAsHtml(YUIComponent component) {
        if (component instanceof YUIComponentZoneImpl) {
            return getComponentZoneAsHtml((YUIComponentZoneImpl) component);
        } else if (component instanceof YUIUnitImpl) {
            return getComponentUnitAsHtml((YUIUnitImpl) component);
        }
        return null;
    }

    private String getComponentZoneAsHtml(YUIComponentZoneImpl component) {
        StringBuilder sb = new StringBuilder();

        sb.append("\t\t\t\t<div class=\"" + component.getCSS() + "\">\n");

        for (YUIComponent zonecomp : component.getComponents()) {
            sb.append(getComponentAsHtml(zonecomp));
        }

        sb.append("\t\t\t\t</div>\n");

        return sb.toString();
    }

    private String getComponentUnitAsHtml(YUIUnitImpl component) {
        return "\t\t\t\t\t<div class=\"" + component.getCSS()
                + "\"><!-- WebContents here --></div>\n";
    }
}

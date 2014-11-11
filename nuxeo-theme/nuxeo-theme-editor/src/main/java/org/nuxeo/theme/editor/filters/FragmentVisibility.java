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

package org.nuxeo.theme.editor.filters;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.rendering.StandaloneFilter;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeFamily;

public class FragmentVisibility extends StandaloneFilter {

    private static final Log log = LogFactory.getLog(FragmentVisibility.class);

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        final Element element = info.getElement();
        if (element instanceof Fragment) {

            final String viewMode = info.getViewMode();
            if (viewMode != null && viewMode.startsWith("area-styles")) {
                return null;
            }

            final Fragment fragment = (Fragment) element;
            final PerspectiveType perspective = ThemeManager.getPerspectiveByUrl(info.getThemeUrl());

            final FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(
                    TypeFamily.FORMAT, "widget");
            final Format format = ElementFormatter.getFormatByType(fragment,
                    widgetType);
            if (format == null) {
                return info;
            }

            if ("fragment".equals(viewMode) || "layout".equals(viewMode)) {
                StringBuilder content = new StringBuilder();
                content.append("<div class=\"nxthemesFragment\">");
                String description = fragment.getDescription();
                if (description != null) {
                    content.append(String.format("<div><b>%s</b></div>", description));
                }
                content.append(String.format("%s / %s",
                        fragment.getFragmentType().getTypeName(),
                        format.getName()));
                content.append("</div>");
                info.setMarkup(content.toString());
                return info;
            }

            if (perspective != null) {
                if (!fragment.isVisibleInPerspective(perspective)) {
                    StringBuilder content = new StringBuilder();
                    content.append("<div class=\"nxthemesFragment\">");
                    String description = fragment.getDescription();
                    if (description != null) {
                        content.append(String.format("<div><b>%s</b></div>", description));
                    }
                    content.append(String.format("%s / %s",
                            fragment.getFragmentType().getTypeName(),
                            format.getName()));
                    content.append(" (only visible in: ");
                    final Iterator<PerspectiveType> it = fragment.getVisibilityPerspectives().iterator();
                    while (it.hasNext()) {
                        final PerspectiveType p = it.next();
                        content.append(p.getTitle());
                        if (it.hasNext()) {
                            content.append(", ");
                        }
                    }
                    content.append(")</div>");
                    info.setMarkup(content.toString());
                }
            }

        } else {
            log.warn("The 'fragment visibility' filter can only be applied to fragments.");
        }
        return info;
    }

}

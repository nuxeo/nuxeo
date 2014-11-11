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

import java.util.regex.Pattern;

import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.rendering.StandaloneFilter;

public final class CreateInitialSection extends StandaloneFilter {

    static final Pattern firstTagPattern = Pattern.compile("<(.*?)>",
            Pattern.DOTALL);

    static final Pattern otherTagsPattern = Pattern.compile("<.*?>(.*)",
            Pattern.DOTALL);

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        final Element element = info.getElement();
        if (!(element instanceof PageElement)) {
            return info;
        }
        final String viewMode = info.getViewMode();
        if (!"layout".equals(viewMode)) {
            return info;
        }
        final StringBuilder html = new StringBuilder();
        if (element.hasChildren()) {
            return info;
        }
        html.append(
                "<a href=\"javascript:void(0)\" class=\"nxthemesAddSection\" title=\"Add a section\"><div sectionid=\"").append(
                info.getElement().getUid().toString()).append(
                "\"> Add a section</div></a>");
        info.setMarkup(html.toString());
        return info;
    }

}

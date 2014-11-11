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

import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.html.filters.layout.DefaultLayoutView;
import org.nuxeo.theme.rendering.RenderingInfo;

public class UnregisteredWidget extends DefaultLayoutView {

    @Override
    public String render(final RenderingInfo info) {
        final Element element = info.getElement();
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "<div class=\"nxthemesUnregisteredWidget\" id=\"e%s\">",
                element.getUid()));
        String description = element.getDescription();
        if (description != null) {
            sb.append(String.format("<div><b>%s</b></div>", description));
        }
        sb.append(String.format("Unregistered widget view: <b>%s</b>",
                info.getFormat().getName()));
        sb.append("</div>");
        return sb.toString();
    }
}

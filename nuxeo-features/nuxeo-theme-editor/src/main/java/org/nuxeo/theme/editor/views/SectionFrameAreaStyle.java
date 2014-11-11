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

import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.views.AbstractView;

public class SectionFrameAreaStyle extends AbstractView {

    @Override
    public String render(final RenderingInfo info) {
        final StringBuilder sb = new StringBuilder();
        sb.append(
                "<table summary=\"\" cellpadding=\"0\" cellspacing=\"0\" style=\"width: 100%\">").append(
                "<tr><td class=\"nxthemesAreaStyle\">");
        sb.append(AreaStyleToolbox.render(info));
        sb.append("</td></tr></table>");
        return sb.toString();
    }

}

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

public class CellFrameWidget extends AbstractView {

    @Override
    public String render(final RenderingInfo info) {
        final String markup = info.getMarkup();
        String className = "nxthemesContainer";
        if (info.getElement().isEmpty()) {
            className = "nxthemesContainer nxthemesEmptyContainer";
        }
        return String.format("<td valign=\"top\" class=\"%s\">%s</td>",
                className, markup);
    }
}

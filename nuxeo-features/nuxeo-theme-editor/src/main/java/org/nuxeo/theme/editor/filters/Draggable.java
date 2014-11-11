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

import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.rendering.StandaloneFilter;

public final class Draggable extends StandaloneFilter {

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        if (info.isRenderingPostponed(cache)) {
            return info;
        }
        info.setMarkup(String.format(
                "<div class=\"nxthemesDraggable\"><div class=\"nxthemesFrameTop\"></div><div class=\"nxthemesFrameRight\"></div><div class=\"nxthemesFrameLeft\"></div><div class=\"nxthemesFrameBottom\"></div>%s</div>",
                info.getMarkup()));
        return info;
    }

}

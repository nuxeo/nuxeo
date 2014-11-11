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

package org.nuxeo.theme.jsf.filters.standalone;

import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.rendering.StandaloneFilter;

public final class FragmentTag extends StandaloneFilter {

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        // postpone the rendering
        if (info.isRenderingPostponed(cache)) {
            String markup = String.format(
                    "<nxthemes:fragment xmlns:nxthemes=\"http://nuxeo.org/nxthemes\" uid=\"%s\" engine=\"%s\" mode=\"%s\" />",
                    info.getElement().getUid().toString(),
                    info.getEngine().getTypeName(), info.getViewMode());
            info.setMarkup(markup);
        }
        return info;
    }

}

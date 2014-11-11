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

package org.nuxeo.theme.jsf;

import org.nuxeo.theme.html.HTMLView;
import org.nuxeo.theme.models.InfoPool;
import org.nuxeo.theme.rendering.RenderingInfo;

public class JSFView extends HTMLView {

    @Override
    public String replaceModelExpressions(final RenderingInfo info,
            final String html) {
        final String infoId = InfoPool.computeInfoId(info);
        return html.replaceAll("nxthemesInfo", String.format(
                "nxthemesInfo\\.map\\.%s", infoId));
    }

}

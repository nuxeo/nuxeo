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

package org.nuxeo.theme.jsf.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.html.HTMLView;
import org.nuxeo.theme.rendering.RenderingInfo;

public class JSFView extends HTMLView {

    private static final Log log = LogFactory.getLog(JSFView.class);

    @Override
    public String render(final RenderingInfo info) {
        log.warn(String.format(
                "JSFView is deprecated. Please remove <class>org.nuxeo.theme.jsf.views.JSFView</class> from <view name=\"%s\">",
                info.getFormat().getName()));
        return super.render(info);
    }

}

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

package org.nuxeo.theme.views;

import org.nuxeo.theme.rendering.RenderingInfo;

public abstract class AbstractView implements View {

    public abstract String render(RenderingInfo info);

    private ViewType viewType;

    public ViewType getViewType() {
        return viewType;
    }

    public void setViewType(final ViewType viewType) {
        this.viewType = viewType;
    }

}

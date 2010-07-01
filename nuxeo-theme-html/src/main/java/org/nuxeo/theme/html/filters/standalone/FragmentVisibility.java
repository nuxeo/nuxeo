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

package org.nuxeo.theme.html.filters.standalone;

import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.rendering.StandaloneFilter;
import org.nuxeo.theme.themes.ThemeManager;

public final class FragmentVisibility extends StandaloneFilter {

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        final Element element = info.getElement();
        if (element.getElementType().getTypeName().equals("fragment")) {
            final Fragment fragment = (Fragment) element;
            final PerspectiveType perspective = ThemeManager.getPerspectiveByUrl(info.getThemeUrl());
            if (perspective != null) {
                if (!fragment.isVisibleInPerspective(perspective)) {
                    return null;
                }
            }
        }
        return info;
    }

}

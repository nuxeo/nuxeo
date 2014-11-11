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
import org.nuxeo.theme.html.CSSUtils;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.rendering.StandaloneFilter;

public class ElementName extends StandaloneFilter {

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        final Element element = info.getElement();
        String name = element.getName();
        String typeName = element.getElementType().getTypeName();
        if (name != null) {
            String markup = CSSUtils.insertCssClass(
                    info.getMarkup(),
                    CSSUtils.toCamelCase(String.format("%s %s", name, typeName)));
            info.setMarkup(markup);
        }
        return info;
    }
}

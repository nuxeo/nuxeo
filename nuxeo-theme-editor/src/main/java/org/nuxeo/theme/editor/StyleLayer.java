/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.editor;

public class StyleLayer {

    private final String name;

    private final Integer uid;

    private final boolean selected;

    public StyleLayer(final String name, final Integer uid,
            final boolean selected) {
        this.name = name;
        this.uid = uid;
        this.selected = selected;
    }

    public String getRendered() {
        final String className = selected ? "selected" : "";
        return String.format(
                "<a href=\"javascript:void(0)\" class=\"%s\" onclick=\"NXThemesStyleEditor.setCurrentStyleLayer(%s)\" >%s</a>",
                className, uid, name);
    }
}

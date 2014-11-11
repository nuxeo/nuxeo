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

public class StyleCategory {

    private final String category;

    private final String title;

    private final boolean selected;

    public StyleCategory(final String category, final String title,
            final boolean selected) {
        this.category = category;
        this.title = title;
        this.selected = selected;
    }

    public String getCategory() {
        return category;
    }

    public String getRendered() {
        final String className = selected ? "selected" : "";
        return String.format(
                "<a href=\"javascript:void(0)\" class=\"%s\" onclick=\"NXThemesStyleEditor.setStylePropertyCategory('%s')\">%s</a>\n",
                className, category, title);
    }
}

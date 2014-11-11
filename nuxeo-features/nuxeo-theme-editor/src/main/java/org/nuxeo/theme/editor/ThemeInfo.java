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

public class ThemeInfo {
    public String name;

    public String path;

    public boolean selected;

    public ThemeInfo(String name, String path, boolean selected) {
        this.name = name;
        this.path = path;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isSelected() {
        return selected;
    }

}

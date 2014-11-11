/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf;

import java.io.Serializable;

/**
 * Holds needed information about a content view to select it from UI, or to
 * selected one of its result layouts from UI
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class ContentViewHeader implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String title;

    protected boolean translateTitle;

    protected String iconPath;

    public ContentViewHeader(String name, String title, boolean translateTitle,
            String iconPath) {
        this.name = name;
        this.title = title;
        this.translateTitle = translateTitle;
        this.iconPath = iconPath;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public boolean isTranslateTitle() {
        return translateTitle;
    }

    public String getIconPath() {
        return iconPath;
    }

    @Override
    public String toString() {
        return String.format("ContentViewHeader [name=%s, title=%s, "
                + "translateTitle=%s, iconPath=%s]", name, title,
                Boolean.valueOf(translateTitle), iconPath);
    }

}

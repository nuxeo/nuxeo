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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("layout")
public class ContentViewLayoutImpl implements ContentViewLayout {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@title")
    protected String title;

    @XNode("@translateTitle")
    protected boolean translateTitle;

    @XNode("@iconPath")
    protected String iconPath;

    @XNode("@showCSVExport")
    protected boolean showCSVExport = false;

    @XNode("@showPDFExport")
    protected boolean showPDFExport = false;

    public ContentViewLayoutImpl() {
    }

    public ContentViewLayoutImpl(String name, String title,
            boolean translateTitle, String iconPath, boolean showCSVExport) {
        this.name = name;
        this.title = title;
        this.translateTitle = translateTitle;
        this.iconPath = iconPath;
        this.showCSVExport = showCSVExport;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public boolean getTranslateTitle() {
        return translateTitle;
    }

    @Override
    public boolean getShowCSVExport() {
        return showCSVExport;
    }

    @Override
    public boolean getShowPDFExport() {
        return showPDFExport;
    }

    @Override
    public String toString() {
        return String.format("ContentViewLayoutImpl [name=%s, title=%s, "
                + "translateTitle=%s, iconPath=%s, showCSVExport=%s]", name,
                title, Boolean.valueOf(translateTitle), iconPath, new Boolean(
                        showCSVExport));
    }

}

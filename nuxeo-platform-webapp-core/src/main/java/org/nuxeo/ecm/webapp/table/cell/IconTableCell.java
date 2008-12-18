/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.table.cell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Support for displaying an icon and a string next to it.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public class IconTableCell extends TableCell {
    private static final long serialVersionUID = -5797880872899749786L;

    private static final Log log = LogFactory.getLog(IconTableCell.class);

    protected String iconText;

    protected String iconAlt;

    /**
     * Only the icon will be displayed.
     */
    public IconTableCell(String iconPath) {
        super(iconPath);

        log.debug("Constructed with icon path: " + iconPath);
    }

    /**
     * The icon and a string will be displayed.
     */
    public IconTableCell(String iconPath, String displayedStringValue) {
        super(iconPath);

        iconText = displayedStringValue;

        log.debug("Constructed with icon path: " + iconPath
                + ", and string value: " + displayedStringValue);
    }

    /**
     * The icon and a string will be displayed.
     */
    public IconTableCell(String iconPath, String displayedStringValue, String draggableId, boolean dropable) {
        super(iconPath);

        iconText = displayedStringValue;
        setCellId(draggableId);
        setDropable(dropable);

        log.debug("Constructed with icon path: " + iconPath
                + ", and string value: " + displayedStringValue
                + ", and draggable id: " + draggableId);
    }

    /**
     * The icon and a string will be displayed plus an alt attribute.
     */
    public IconTableCell(String iconPath, String displayedStringValue, String alt, String draggableId, boolean dropable) {
        super(iconPath);

        iconText = displayedStringValue;
        setCellId(draggableId);
        setDropable(dropable);
        iconAlt = alt;

        log.debug("Constructed with icon path: " + iconPath
                + ", and string value: " + displayedStringValue
                + ", and alt attribute: " + alt
                + ", and draggable id: " + draggableId);
    }

    public String getIconText() {
        return iconText;
    }

    public String getIconAlt() {
        return iconAlt;
    }

    public void setIconText(String iconText) {
        this.iconText = iconText;
    }

    /**
     * Support for image alt attribute.
     *
     * @param iconAlt the value for the alt attribute
     */
    public void setIconAlt(String iconAlt) {
        this.iconAlt = iconAlt;
    }

}

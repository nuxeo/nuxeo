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

package org.nuxeo.ecm.webapp.table.header;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The column header for icons. Support to display an icon iside the table
 * column header.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public class IconColHeader extends TableColHeader {
    private static final long serialVersionUID = 3797307826263034490L;

    private static final Log log = LogFactory.getLog(IconColHeader.class);

    protected String defaultIconPath;

    public IconColHeader(String label, String iconPath, String id) {
        super(label, id);

        defaultIconPath = iconPath;
        log.debug("Constructed with iconPath: " + iconPath);
    }

    public String getDefaultIconPath() {
        return defaultIconPath;
    }

    public void setDefaultIconPath(String iconPath) {
        defaultIconPath = iconPath;
    }

}

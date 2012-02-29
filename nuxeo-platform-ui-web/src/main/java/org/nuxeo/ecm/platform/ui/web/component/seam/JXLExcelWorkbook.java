/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.component.seam;

import java.util.Date;

import javax.faces.component.UIComponent;

import org.jboss.seam.excel.WorksheetItem;

/**
 * Overrides default JXL workbook to avoid dumb cache on component id
 *
 * @since 5.6
 */
public class JXLExcelWorkbook extends org.jboss.seam.excel.jxl.JXLExcelWorkbook {

    public void addItem(WorksheetItem item) {
        // cheat by changing the component id before calling addItem to avoid
        // going through the cache...
        UIComponent comp = (UIComponent) item;
        String oldId = comp.getId();
        long timestamp = new Date().getTime();
        comp.setId(String.valueOf("NX_MARKER_" + timestamp));
        try {
            super.addItem(item);
        } finally {
            comp.setId(oldId);
        }
    }

}

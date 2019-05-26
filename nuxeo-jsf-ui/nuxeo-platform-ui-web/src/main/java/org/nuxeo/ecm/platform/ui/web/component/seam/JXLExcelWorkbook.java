/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    @Override
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

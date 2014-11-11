/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: SortableSelectDocumentDataModelImpl.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model.impl;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;

/**
 * Sortable select document data model that performs sorts assuming rows are
 * SelectModelRow containment DocumentModel data.
 * <p>
 * The column sort criterion should be the string 'schemaName:fieldName' like
 * for instance 'dublincore:title'.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class SortableSelectDocumentDataModelImpl extends
        AbstractSortableSelectDataModel {

    private static final long serialVersionUID = 8096546666470084522L;

    private static final Log log = LogFactory
            .getLog(SortableSelectDocumentDataModelImpl.class);

    public SortableSelectDocumentDataModelImpl(String name, List data,
            List selectedData, String defaultSortColumn) {
        super(name, data, selectedData, defaultSortColumn);
    }

    @Override
    public List<SelectDataModelRow> getRows() {
        sort(sort, ascending);
        return super.getRows();
    }

    @Override
    public boolean isDefaultAscending(String sortColumn) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void sort(String column, boolean ascending) {
        if (column != null) {
            String[] s = column.split(":");
            String schemaName = null;
            String fieldName = null;
            try {
                schemaName = s[0];
                fieldName = s[1];
            } catch (ArrayIndexOutOfBoundsException err) {
                log.error("Invalid column name '" + column
                        + "', should follow format 'schemaName:fieldName'");
            }

            if (schemaName == null || fieldName == null) {
                // don't bother sorting
                log.error("Could not sort rows");
            } else {
                Collections.sort(rows,
                        new SelectDocumentDataModelRowComparator(schemaName,
                                fieldName, ascending));
            }
        } else {
            log.error("Null column received");
        }
    }

}

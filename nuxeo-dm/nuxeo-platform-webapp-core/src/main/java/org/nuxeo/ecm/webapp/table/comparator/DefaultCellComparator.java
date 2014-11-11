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

package org.nuxeo.ecm.webapp.table.comparator;

import java.util.Comparator;
import java.io.Serializable;

import org.nuxeo.ecm.webapp.table.cell.AbstractTableCell;
import org.nuxeo.ecm.webapp.table.row.TableRow;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class DefaultCellComparator implements Comparator<TableRow>, Serializable {

    private static final long serialVersionUID = -6492431263699827014L;

    protected final int columnIndex;

    protected int ascending = -1;

    public DefaultCellComparator(int columnIndex, boolean ascending) {
        this.columnIndex = columnIndex;

        this.ascending = ascending ? 1 : -1;
    }

    /**
     * Compares two cells given two table rows.
     */
    public int compare(TableRow raw1, TableRow raw2) {
        int result = 0;
        AbstractTableCell cell1 = raw1.getCells().get(columnIndex);
        AbstractTableCell cell2 = raw2.getCells().get(columnIndex);

        if (cell1 == null && cell2 != null) {
            result = -1;
        } else {
            if (cell1 == null && cell2 == null) {
                result = 0;
            } else {
                if (cell1 != null && cell2 == null) {
                    result = 1;
                } else {
                    result = cell1.compareTo(cell2) * ascending;
                }
            }
        }

        return result;
    }
}

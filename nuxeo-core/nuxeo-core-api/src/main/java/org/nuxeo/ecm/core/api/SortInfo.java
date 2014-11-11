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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;

/**
 * This class holds the details for sorting.
 *
 * @author <a href='mailto:glefter@nuxeo.com'>George Lefter</a>
 */
public class SortInfo implements Serializable {

    private static final long serialVersionUID = -5490026543290755342L;

    private final String sortColumn;

    private final boolean sortAscending;

    /**
     * @param sortColumn the column to sort by, in schema:field format
     * @param sortAscending whether to sort ascending or descending
     */
    public SortInfo(String sortColumn, boolean sortAscending) {
        if (sortColumn == null) {
            throw new IllegalArgumentException("sortColumn cannot be null");
        }
        this.sortColumn = sortColumn;
        this.sortAscending = sortAscending;
    }

    public boolean getSortAscending() {
        return sortAscending;
    }

    /**
     * @return the column to sort by, in schema:field format
     */
    public String getSortColumn() {
        return sortColumn;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SortInfo) {
            SortInfo other = (SortInfo) obj;
            if (sortColumn != null && sortColumn.equals(other.sortColumn)) {
                return sortAscending == other.sortAscending;
            } else if (sortColumn == null && other.sortColumn == null) {
                return sortAscending == other.sortAscending;
            }
            return false;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("SortInfo [sortColumn=%s, sortAscending=%s]",
                sortColumn, Boolean.valueOf(sortAscending));
    }

}

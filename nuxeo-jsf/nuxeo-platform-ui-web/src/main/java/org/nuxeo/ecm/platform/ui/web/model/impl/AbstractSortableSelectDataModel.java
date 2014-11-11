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
 * $Id: AbstractSortableSelectDataModel.java 28925 2008-01-10 14:39:42Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model.impl;

import java.util.List;

import org.nuxeo.ecm.platform.ui.web.model.SortableDataModel;

/**
 * Provides support for sorting table models. Inspired from Tomahawk examples.
 * Abstract method design pattern.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public abstract class AbstractSortableSelectDataModel extends
        SelectDataModelImpl implements SortableDataModel {

    private static final long serialVersionUID = 7945731561137032048L;

    public static final int SORT_ASCENDING = 1;

    public static final int SORT_DESCENDING = -1;

    protected String sort;

    protected boolean ascending;

    protected AbstractSortableSelectDataModel() {
    }

    protected AbstractSortableSelectDataModel(String name, List data,
            List selectedData, String defaultSortColumn) {
        super(name, data, selectedData);
        sort = defaultSortColumn;
        ascending = isDefaultAscending(defaultSortColumn);
    }

    /**
     * Sorts the list. Should be implemented by the children to customize the
     * sort (what comparators should be used, what other condition must be met).
     */
    public abstract void sort(String column, boolean ascending);

    /**
     * Is the default sort direction for the given column "ascending"?
     */
    public abstract boolean isDefaultAscending(String sortColumn);

    public void sort(String sortColumn) {
        if (sortColumn == null) {
            throw new IllegalArgumentException(
                    "Argument sortColumn must not be null.");
        }

        if (sort.equals(sortColumn)) {
            // current sort equals new sortColumn -> reverse sort order
            ascending = !ascending;
        } else {
            // sort new column in default direction
            sort = sortColumn;
            ascending = isDefaultAscending(sort);
        }

        sort(sort, ascending);
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

}

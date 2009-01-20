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

package org.nuxeo.ecm.webapp.table.sort;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;

/**
 * Provides support for sorting table models. Inspired from Tomahawk examples.
 * Abstract method design pattern.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public abstract class SortableTableModel implements Serializable {
    public static final int SORT_ASCENDING = 1;

    public static final int SORT_DESCENDING = -1;

    private static final long serialVersionUID = -3516230980687196803L;

    private String sort;

    private boolean ascending;


    protected SortableTableModel() {
    }

    protected SortableTableModel(String defaultSortColumn) {
        sort = defaultSortColumn;
        ascending = isDefaultAscending(defaultSortColumn);
    }

    /**
     * Sort the list. Should be implemented by the children to customize the
     * sort (what comparators should be used, what other condition must be met).
     */
    protected abstract void sort(String column, boolean ascending);

    /**
     * Is the default sort direction for the given column "ascending" ?
     */
    protected abstract boolean isDefaultAscending(String sortColumn);

    /**
     * Sorts the table.
     *
     * @param event
     */
    public void doSort(ActionEvent event) {

        //TODO: need to refactor this
        HttpServletRequest request = (HttpServletRequest) FacesContext
                .getCurrentInstance().getExternalContext().getRequest();
        String label = request.getParameter("columnLabel");

        if (label == null) {
            throw new IllegalArgumentException("Argument sortColumn must not be null.");
        }

        if (label.equals(sort)) {
            //current sort equals new sortColumn -> reverse sort order
            ascending = !ascending;
        } else {
            //sort new column in default direction
            sort = label;
            ascending = isDefaultAscending(label);
        }

        sort(label, ascending);
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

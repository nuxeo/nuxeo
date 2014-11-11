/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class holds the details for sorting.
 *
 * @author <a href='mailto:glefter@nuxeo.com'>George Lefter</a>
 */
public class SortInfo implements Serializable {

    private static final long serialVersionUID = -5490026543290755342L;

    private static final Log log = LogFactory.getLog(SortInfo.class);

    public static final String SORT_COLUMN_NAME = "sortColumn";

    public static final String SORT_ASCENDING_NAME = "sortAscending";

    protected String sortColumn;

    protected boolean sortAscending;

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

    /**
     * @since 5.4.0
     */
    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

    /**
     * @since 5.4.0
     */
    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
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

    /**
     * Returns a map for given sort info, or null if sort info is null.
     * <p>
     * The map keys are {@link #SORT_COLUMN_NAME} and
     * {@link #SORT_ASCENDING_NAME}.
     *
     * @since 5.4.0
     */
    public static Map<String, Serializable> asMap(SortInfo sortInfo) {
        if (sortInfo == null) {
            return null;
        }
        Map<String, Serializable> res = new HashMap<String, Serializable>();
        res.put(SORT_COLUMN_NAME, sortInfo.getSortColumn());
        res.put(SORT_ASCENDING_NAME,
                Boolean.valueOf(sortInfo.getSortAscending()));
        return res;
    }

    /**
     * Returns a sort info for given map, or null if map is null or does not
     * contain both keys {@link #SORT_COLUMN_NAME} and
     * {@link #SORT_ASCENDING_NAME}.
     *
     * @since 5.4.0
     */
    public static SortInfo asSortInfo(Map<String, Serializable> map) {
        if (map == null) {
            return null;
        }
        if (map.containsKey(SORT_COLUMN_NAME)
                && map.containsKey(SORT_ASCENDING_NAME)) {
            return new SortInfo(
                    (String) map.get("sortColumn"),
                    Boolean.parseBoolean(String.valueOf(map.get("sortAscending"))));
        } else {
            log.error("Cannot resolve sort info from map: " + map);
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("SortInfo [sortColumn=%s, sortAscending=%s]",
                sortColumn, Boolean.valueOf(sortAscending));
    }

}

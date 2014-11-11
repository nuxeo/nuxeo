/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

/**
 * The different types of selections available, and information about what they
 * correspond to in the database.
 */
public enum SelectionType {

    /**
     * Selection for the children of a given parent id.
     */
    CHILDREN(Model.HIER_TABLE_NAME, Model.HIER_PARENT_KEY,
            Model.HIER_CHILD_NAME_KEY, Model.HIER_CHILD_ISPROPERTY_KEY,
            Invalidations.PARENT),

    /**
     * Selection for the versions of a given version series.
     */
    SERIES_VERSIONS(Model.VERSION_TABLE_NAME, Model.VERSION_VERSIONABLE_KEY,
            null, null, "__SERIES_VERSIONS__"),

    /**
     * Selection for the proxies of a given version series.
     */
    SERIES_PROXIES(Model.PROXY_TABLE_NAME, Model.PROXY_VERSIONABLE_KEY, null,
            null, Invalidations.SERIES_PROXIES),

    /**
     * Selection for the proxies of a given target.
     */
    TARGET_PROXIES(Model.PROXY_TABLE_NAME, Model.PROXY_TARGET_KEY, null, null,
            Invalidations.TARGET_PROXIES);

    /**
     * The table name for this selection.
     */
    public final String tableName;

    /**
     * The key for the selection id.
     * <p>
     * For instance for a children selection this is the parent id.
     */
    public final String selKey;

    /**
     * The key to use to additionally filter on fragment values.
     * <p>
     * For instance for a children selection this is the child name.
     */
    public final String filterKey;

    /**
     * The key to use to additionally filter on criterion.
     * <p>
     * For instance for a children selection this is the complex property flag.
     * <p>
     * This can be {@code null} for no criterion filtering.
     */
    public final String criterionKey;

    /**
     * Pseudo-table to use to notify about selection invalidation.
     */
    public final String invalidationTableName;

    private SelectionType(String tableName, String selKey, String filterKey,
            String criterionKey, String invalidationTableName) {
        this.tableName = tableName;
        this.selKey = selKey;
        this.filterKey = filterKey;
        this.criterionKey = criterionKey;
        this.invalidationTableName = invalidationTableName;
    }
}
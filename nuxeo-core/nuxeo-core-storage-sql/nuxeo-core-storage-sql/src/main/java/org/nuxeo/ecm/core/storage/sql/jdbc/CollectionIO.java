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
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Row;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Interface for a class that knows how to get a collection's value from a
 * result set, and set a collection's values to a prepared statement (and
 * execute it).
 */
public interface CollectionIO {

    /**
     * Gets one value from the current position of the result set.
     */
    Serializable getCurrentFromResultSet(ResultSet rs, List<Column> columns,
            Model model, Serializable[] returnId, int[] returnPos)
            throws SQLException;

    /**
     * Sets the values of a fragment to a SQL prepared statement, and executes
     * the statement for each value. Uses batching if possible.
     */
    void executeInserts(PreparedStatement ps, List<Row> rows,
            List<Column> columns, boolean supportsBatchUpdates, String sql,
            JDBCConnection connection) throws SQLException;

}

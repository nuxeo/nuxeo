/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowUpdate;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Interface for a class that knows how to get a collection's value from a result set, and set a collection's values to
 * a prepared statement (and execute it).
 */
public interface CollectionIO {

    /**
     * Gets one value from the current position of the result set.
     */
    Serializable getCurrentFromResultSet(ResultSet rs, List<Column> columns, Model model, Serializable[] returnId,
            int[] returnPos) throws SQLException;

    /**
     * Sets the values of a fragment to a SQL prepared statement, and executes the statement for each value. Uses
     * batching if possible.
     */
    void executeInserts(PreparedStatement ps, List<RowUpdate> rowus, List<Column> columns, boolean supportsBatchUpdates,
            String sql, JDBCConnection connection) throws SQLException;

}

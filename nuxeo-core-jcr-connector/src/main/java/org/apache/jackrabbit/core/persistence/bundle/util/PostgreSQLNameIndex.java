/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.persistence.bundle.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Same as {@link DbNameIndex} but does not make use of the
 * {@link java.sql.Statement#RETURN_GENERATED_KEYS} feature as it is not
 * provided by the underlying database driver for PostgreSQL.
 */
public class PostgreSQLNameIndex extends DbNameIndex {

    protected PreparedStatement generatedKeySelect;

    public PostgreSQLNameIndex(Connection con, String schemaObjectPrefix)
            throws SQLException {
        super(con, schemaObjectPrefix);
    }

    /**
     * Inits this index and prepares the statements.
     *
     * @param con the jdbc connection
     * @param schemaObjectPrefix the prefix for table names
     * @throws SQLException if the statements cannot be prepared.
     */
    protected void init(Connection con, String schemaObjectPrefix)
            throws SQLException {
        nameSelect = con.prepareStatement("select NAME from " + schemaObjectPrefix + "NAMES where ID = ?");
        indexSelect = con.prepareStatement("select ID from " + schemaObjectPrefix + "NAMES where NAME = ?");
        nameInsert = con.prepareStatement("insert into " + schemaObjectPrefix + "NAMES (NAME) values (?)");
        generatedKeySelect = con.prepareStatement("select currval('" + schemaObjectPrefix + "NAMES_ID_SEQ')");
    }

    /**
     * Inserts a string into the database and returns the new index.
     * <p/>
     * Instead of using the {@link java.sql.Statement#RETURN_GENERATED_KEYS}
     * feature, the newly inserted index is retrieved by a 2nd select statement.
     *
     * @param string the string to insert
     * @return the new index.
     */
    protected int insertString(String string) {
        // assert index does not exist
        try {
            nameInsert.setString(1, string);
            nameInsert.executeUpdate();
            return getGeneratedKey();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to insert index: " + e);
        }
    }

    /**
     * Retrieves the last assigned key from the database.
     * @return the index.
     */
    protected int getGeneratedKey() {
        try {
            ResultSet rs = generatedKeySelect.executeQuery();
            try {
                if (!rs.next()) {
                    return -1;
                } else {
                    return rs.getInt(1);
                }
            } finally {
                rs.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read index: " + e);
        }
    }

}
